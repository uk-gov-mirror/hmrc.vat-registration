/*
 * Copyright 2020 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package controllers

import java.time.LocalDate

import common.RegistrationId
import common.exceptions.MissingRegDocument
import fixtures.VatRegistrationFixture
import helpers.VatRegSpec
import models.api._
import org.mockito.ArgumentMatchers.{any, anyString}
import org.mockito.Mockito.when
import play.api.http.Status
import play.api.libs.json.{JsObject, JsValue, Json}
import play.api.mvc.Result
import play.api.test.FakeRequest
import repositories.RegistrationMongoRepository
import services.{SubmissionService, VatRegistrationService}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.{HeaderCarrier, Upstream5xxResponse}

import scala.concurrent.Future

class VatRegistrationControllerSpec extends VatRegSpec with VatRegistrationFixture {

  import play.api.test.Helpers._

  val vatLodgingOfficer: LodgingOfficer = LodgingOfficer(
    nino = "NB666666C",
    role = "director",
    name = name,
    details = None
  )

  class Setup {
    val controller: VatRegistrationController = new VatRegistrationController(mockVatRegistrationService,
                                                                              mockSubmissionService,
                                                                              mockRegistrationMongoRepository,
                                                                              mockAuthConnector,
                                                                              stubControllerComponents()) {

      override val registrationRepository: RegistrationMongoRepository = mockRegistrationMongoRepository
      override val resourceConn: RegistrationMongoRepository = mockRegistrationMongoRepository
    }
  }

  val registrationId = "reg-12345"

  "GET /" should {

    "return 403 if user not authenticated for newVatRegistration" in new Setup {
      AuthorisationMocks.mockAuthenticatedLoggedInNoCorrespondingData()

      controller.newVatRegistration(FakeRequest()) returnsStatus FORBIDDEN
    }

    "return 201 if a new VAT scheme is successfully created" in new Setup {
      AuthorisationMocks.mockAuthenticated(internalid)

      ServiceMocks.mockSuccessfulCreateNewRegistration(regId, internalid)
      controller.newVatRegistration()(FakeRequest()) returnsStatus CREATED
    }

    "call to retrieveVatScheme return Ok with VatScheme" in new Setup {
      AuthorisationMocks.mockAuthorised(regId.value, internalid)
      ServiceMocks.mockRetrieveVatScheme(regId, vatScheme)

      controller.retrieveVatScheme(regId)(FakeRequest()) returnsStatus OK
    }

    "call to retrieveVatScheme return ServiceUnavailable" in new Setup {
      AuthorisationMocks.mockAuthorised(regId.value, internalid)
      ServiceMocks.mockRetrieveVatSchemeThrowsException(regId)

      controller.retrieveVatScheme(regId)(FakeRequest()) returnsStatus SERVICE_UNAVAILABLE
    }

    "return 503 if RegistrationService encounters any problems" in new Setup {
      AuthorisationMocks.mockAuthorised(regId.value, internalid)
      ServiceMocks.mockFailedCreateNewRegistration(regId, internalid)

      controller.newVatRegistration()(FakeRequest()) returnsStatus SERVICE_UNAVAILABLE
    }

    "return 503 if RegistrationService encounters any problems with the DB" in new Setup {
      AuthorisationMocks.mockAuthorised(regId.value, internalid)
      ServiceMocks.mockFailedCreateNewRegistrationWithDbError(regId, internalid)

      controller.newVatRegistration()(FakeRequest()) returnsStatus SERVICE_UNAVAILABLE
    }
    "updateBankAccountDetails" should {

      val registrationId = "reg-12345"

      val accountNumber = "12345678"
      val sortCode = "12-34-56"
      val bankAccountDetails = BankAccountDetails("testAccountName", sortCode, accountNumber)
      val bankAccount = BankAccount(true, Some(bankAccountDetails))

      "return a 200 if the update to mongo was successful" in new Setup {
        AuthorisationMocks.mockAuthorised(regId.value, internalid)
        when(mockRegistrationMongoRepository.updateBankAccount(any(), any())(any()))
          .thenReturn(Future.successful(bankAccount))

        val request: FakeRequest[JsObject] = FakeRequest().withBody(
          Json.obj(
            "isProvided" -> true,
            "details" -> Json.obj(
              "name" -> "testAccountName",
              "sortCode" -> sortCode,
              "number" -> accountNumber
            )
          )
        )

        val result: Future[Result] = controller.updateBankAccountDetails(regId.value)(request)
        status(result) mustBe OK
      }
    }

    "fetchBankAccountDetails" should {
      val registrationId = "reg-12345"
      val accountNumber = "12345678"
      val sortCode = "12-34-56"
      val bankAccountDetails = BankAccountDetails("testAccountName", sortCode, accountNumber)
      val bankAccount = BankAccount(true, Some(bankAccountDetails))

      "return a 200 if the fetch from mongo was successful" in new Setup {
        AuthorisationMocks.mockAuthorised(regId.value, internalid)
        when(mockRegistrationMongoRepository.fetchBankAccount(any())(any()))
          .thenReturn(Future.successful(Some(bankAccount)))

        val expected: JsObject = Json.obj(
          "isProvided" -> true,
          "details" -> Json.obj(
            "name" -> "testAccountName",
            "sortCode" -> sortCode,
            "number" -> accountNumber
          )
        )

        val result: Future[Result] = controller.fetchBankAccountDetails(regId.value)(FakeRequest())
        status(result) mustBe OK
        contentAsJson(result) mustBe expected
      }

      "return a 404 if the fetch from mongo returned nothing" in new Setup {
        AuthorisationMocks.mockAuthorised(regId.value, internalid)
        when(mockRegistrationMongoRepository.fetchBankAccount(any())(any()))
          .thenReturn(Future.successful(None))

        val result: Future[Result] = controller.fetchBankAccountDetails(regId.value)(FakeRequest())
        status(result) mustBe NOT_FOUND

      }
    }

    "fetchReturns" should {
      val registrationId = "reg-12345"
      val date = StartDate(Some(LocalDate.of(2017, 1, 1)))
      val returns = Returns(true, "quarterly", Some("jan"), date)

      val expected = Json.obj(
        "reclaimVatOnMostReturns" -> true,
        "frequency" -> "quarterly",
        "staggerStart" -> "jan",
        "start" -> date
      )

      "return a OK if the returns is present in the database" in new Setup {
        AuthorisationMocks.mockAuthorised(regId.value, internalid)
        when(mockRegistrationMongoRepository.fetchReturns(any())(any()))
          .thenReturn(Future.successful(Some(returns)))

        val result: Future[Result] = controller.fetchReturns(regId.value)(FakeRequest())
        status(result) mustBe OK
        contentAsJson(result) mustBe expected
      }

      "return a NOT_FOUND if the returns is not present" in new Setup {
        AuthorisationMocks.mockAuthorised(regId.value, internalid)
        when(mockRegistrationMongoRepository.fetchReturns(any())(any()))
          .thenReturn(Future.successful(None))

        val result: Future[Result] = controller.fetchReturns(regId.value)(FakeRequest())
        status(result) mustBe NOT_FOUND
      }
    }

    "updateReturns" should {

      import Returns._

      val registrationId = "reg-12345"
      val startDate = StartDate(Some(LocalDate of(1990, 10, 10)))
      val returns: Returns = Returns(reclaimVatOnMostReturns = true, MONTHLY, Some(JAN), startDate)

      "return a 200 if the update to mongo is successful" in new Setup {
        AuthorisationMocks.mockAuthorised(regId.value, internalid)
        when(mockRegistrationMongoRepository.updateReturns(any(), any())(any()))
          .thenReturn(Future.successful(returns))

        val request: FakeRequest[JsObject] = FakeRequest().withBody(Json.obj(
          "reclaimVatOnMostReturns" -> true,
          "frequency" -> MONTHLY,
          "staggerStart" -> JAN,
          "start" -> Some(startDate))
        )

        val result: Future[Result] = controller.updateReturns(regId.value)(request)
        status(result) mustBe OK
      }
    }

    "getAcknowledgementReference" should {

      "call getAcknowledgementReference return Ok with Acknowledgement Reference" in new Setup {
        AuthorisationMocks.mockAuthorised(regId.value, internalid)
        ServiceMocks.mockGetAcknowledgementReference(ackRefNumber)

        controller.getAcknowledgementReference(regId)(FakeRequest()) returnsStatus OK
      }

      "call getAcknowledgementReference return ServiceUnavailable" in new Setup {
        AuthorisationMocks.mockAuthorised(regId.value, internalid)
        ServiceMocks.mockGetAcknowledgementReferenceServiceUnavailable(exception)

        controller.getAcknowledgementReference(regId)(FakeRequest()) returnsStatus SERVICE_UNAVAILABLE
      }

      "call getAcknowledgementReference return AcknowledgementReferenceExists Error" in new Setup {
        AuthorisationMocks.mockAuthorised(regId.value, internalid)
        ServiceMocks.mockGetAcknowledgementReferenceExistsError()

        controller.getAcknowledgementReference(regId)(FakeRequest()) returnsStatus CONFLICT
      }
    }

    "Calling getDocumentStatus" should {

      "return a Ok response when the user logged in" in new Setup {
        val json: JsValue = Json.parse(
          """
            | {
            |   "status": "draft",
            |   "ackRef": "testAckRef"
            | }
          """.stripMargin)
        AuthorisationMocks.mockAuthorised(regId.value, internalid)
        ServiceMocks.mockGetDocumentStatus(json)

        val result: Future[Result] = controller.getDocumentStatus(regId)(FakeRequest())
        result returnsStatus OK
        contentAsJson(result) mustBe json
      }

      "return a Not Found response if there is no VAT Registration for the user's ID" in new Setup {
        AuthorisationMocks.mockAuthMongoResourceNotFound(regId.value, internalid)

        status(controller.getDocumentStatus(regId)(FakeRequest())) mustBe NOT_FOUND
      }
    }

    "deleteVatScheme" should {
      "call to deleteVatScheme return Ok with VatScheme" in new Setup {
        AuthorisationMocks.mockAuthorised(regId.value, internalid)
        ServiceMocks.mockDeleteVatScheme("testId")

        status(controller.deleteVatScheme("testId")(FakeRequest())) mustBe OK
      }

      "call to deleteVatScheme return Internal server error" in new Setup {
        AuthorisationMocks.mockAuthorised(regId.value, internalid)
        ServiceMocks.mockDeleteVatSchemeFail("testId")

        status(controller.deleteVatScheme("testId")(FakeRequest())) mustBe INTERNAL_SERVER_ERROR
      }

      "call to deleteVatScheme return Precondition failed" in new Setup {
        AuthorisationMocks.mockAuthorised(regId.value, internalid)
        ServiceMocks.mockDeleteVatSchemeInvalidStatus("testId")

        status(controller.deleteVatScheme("testId")(FakeRequest())) mustBe PRECONDITION_FAILED
      }
    }
  }

  "Calling submitVATRegistration" should {
    "return a Forbidden response if the user is not logged in" in new Setup {
      AuthorisationMocks.mockNotLoggedInOrAuthorised(regId.value)

      val response: Future[Result] = controller.submitVATRegistration(regId)(FakeRequest())
      status(response) mustBe Status.FORBIDDEN
    }

    "return an exception if the Submission Service can't make a DES submission" in new Setup {
      AuthorisationMocks.mockAuthorised(regId.value, internalid)
      ServiceMocks.mockRetrieveVatScheme(regId, vatScheme)

      val idMatcher: RegistrationId = RegistrationId(anyString())

      when(mockSubmissionService.submitVatRegistration(idMatcher)(any[HeaderCarrier]()))
        .thenReturn(Future.failed(Upstream5xxResponse("message", 501, 1)))

      val response: Upstream5xxResponse = intercept[Upstream5xxResponse](await(controller.submitVATRegistration(regId)(FakeRequest())))

      response mustBe Upstream5xxResponse("message", 501, 1)
    }

    "return an Ok response with acknowledgement reference for a valid submit" in new Setup {
      AuthorisationMocks.mockAuthorised(regId.value, internalid)
      ServiceMocks.mockRetrieveVatScheme(regId, vatScheme)
      val idMatcher: RegistrationId = RegistrationId(anyString())

      when(mockSubmissionService.submitVatRegistration(idMatcher)(any[HeaderCarrier]()))
        .thenReturn(Future.successful("BRVT00000000001"))

      val response: Future[Result] = controller.submitVATRegistration(regId)(FakeRequest())
      status(response) mustBe Status.OK
      contentAsJson(response) mustBe Json.toJson("BRVT00000000001")
    }
  }

  "call to clearDownDocument" should {
    "pass" when {
      "given a transactionid" in new Setup {
        when(mockVatRegistrationService.clearDownDocument(any())(any())).thenReturn(Future.successful(true))
        val resp: Future[Result] = controller.clearDownDocument("TransID")(FakeRequest())
        status(resp) mustBe Status.OK
      }
    }
    "fail" when {
      "given a transactionid that isn't found in mongo" in new Setup {
        when(mockVatRegistrationService.clearDownDocument(any())(any())).thenReturn(Future.successful(false))
        val resp: Future[Result] = controller.clearDownDocument("TransID")(FakeRequest())
        status(resp) mustBe Status.INTERNAL_SERVER_ERROR
      }
    }
  }

  "call to saveTransId" should {
    "return Ok" when {
      "the transaction id was saved to the document" in new Setup {
        val regId = "regId"

        when(mockRegistrationMongoRepository.saveTransId(any(), RegistrationId(any()))(any()))
          .thenReturn(Future.successful("transId"))

        lazy val fakeRequest: FakeRequest[JsValue] =
          FakeRequest().withBody[JsValue](Json.parse("""{"transactionID":"transId"}"""))

        val resp: Future[Result] = controller.saveTransId(regId)(fakeRequest)
        status(resp) mustBe Status.OK
      }
    }
  }

  "call to getTurnoverEstimates" should {
    "return a 200 and TurnoverEstimates json when it is returned from the repository" in new Setup {
      AuthorisationMocks.mockAuthorised(regId.value, internalid)
      when(mockVatRegistrationService.getBlockFromEligibilityData[TurnoverEstimates](any())(any(), any()))
        .thenReturn(Future.successful(Some(TurnoverEstimates(None, Some(2024)))))

      val result: Future[Result] = controller.getTurnoverEstimates(regId.value)(FakeRequest())
      val expectedJson: JsValue = Json.obj("turnoverEstimate" -> 2024)

      status(result) mustBe 200
      contentAsJson(result) mustBe expectedJson
    }

    "return a 204 and no json when a None is returned from the repository" in new Setup {
      AuthorisationMocks.mockAuthorised(regId.value, internalid)
      when(mockVatRegistrationService.getBlockFromEligibilityData[TurnoverEstimates](any())(any(), any()))
        .thenReturn(Future.successful(None))

      val result: Future[Result] = controller.getTurnoverEstimates(regId.value)(FakeRequest())
      status(result) mustBe 204
    }

    "return a 404 when a MissingRegDocument exception is thrown" in new Setup {
      AuthorisationMocks.mockAuthorised(regId.value, internalid)
      when(mockVatRegistrationService.getBlockFromEligibilityData[TurnoverEstimates](any())(any(), any()))
        .thenReturn(Future.failed(MissingRegDocument(regId)))

      val result: Future[Result] = controller.getTurnoverEstimates(regId.value)(FakeRequest())
      status(result) mustBe 404
    }
  }

  "call to getThreshold" should {
    "return a 200 and TurnoverEstimates json when it is returned from the repository" in new Setup {
      val threshold: Threshold = Threshold(mandatoryRegistration = true,
        thresholdPreviousThirtyDays = Some(LocalDate.of(2016, 5, 12)),
        thresholdInTwelveMonths = Some(LocalDate.of(2016, 6, 25))
      )

      AuthorisationMocks.mockAuthorised(regId.value, internalid)
      when(mockVatRegistrationService.getBlockFromEligibilityData[Threshold](any())(any(), any()))
        .thenReturn(Future.successful(Some(threshold)))

      val result: Future[Result] = controller.getThreshold(regId.value)(FakeRequest())
      val expectedJson: JsValue = Json.obj("mandatoryRegistration" -> true,
        "thresholdPreviousThirtyDays" -> "2016-05-12",
        "thresholdInTwelveMonths" -> "2016-06-25")

      status(result) mustBe 200
      contentAsJson(result) mustBe expectedJson
    }

    "return a 204 and no json when a None is returned from the repository" in new Setup {
      AuthorisationMocks.mockAuthorised(regId.value, internalid)
      when(mockVatRegistrationService.getBlockFromEligibilityData[Threshold](any())(any(), any()))
        .thenReturn(Future.successful(None))

      val result: Future[Result] = controller.getThreshold(regId.value)(FakeRequest())
      status(result) mustBe 204
    }

    "return a 404 when a MissingRegDocument exception is thrown" in new Setup {
      AuthorisationMocks.mockAuthorised(regId.value, internalid)
      when(mockVatRegistrationService.getBlockFromEligibilityData[Threshold](any())(any(), any()))
        .thenReturn(Future.failed(MissingRegDocument(regId)))

      val result: Future[Result] = controller.getThreshold(regId.value)(FakeRequest())
      status(result) mustBe 404
    }
  }
}
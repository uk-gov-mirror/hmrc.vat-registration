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

package services

import java.time.LocalDate

import common.RegistrationId
import common.exceptions._
import enums.VatRegStatus
import fixtures.VatRegistrationFixture
import helpers.VatRegSpec
import models._
import models.api._
import models.external.CurrentProfile
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import play.api.libs.json.{JsArray, JsObject, JsValue, Json, Reads}
import play.api.test.Helpers._
import repositories.RegistrationMongoRepository
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class VatRegistrationServiceSpec extends VatRegSpec with VatRegistrationFixture {

  class Setup {
    lazy val service: VatRegistrationService = new VatRegistrationService (mockRegistrationMongoRepository, backendConfig, mockHttpClient){
      override lazy val vatCancelUrl = "/test/uri"
      override lazy val vatRestartUrl = "/test-uri"
    }
  }

  implicit val hc: HeaderCarrier = HeaderCarrier()

  //TODO - fix these tests when we define how to create new journeys
  "createNewRegistration" ignore {

    val vatScheme = VatScheme(RegistrationId("1"),internalid, None, None, None, status = VatRegStatus.draft)

    "return a existing VatScheme response " in new Setup {
      val businessRegistrationSuccessResponse = Right(CurrentProfile("1", None, ""))

      when(mockRegistrationMongoRepository.retrieveVatScheme(RegistrationId("1"))).thenReturn(Future.successful(Some(vatScheme)))

      service.createNewRegistration(internalid) returnsRight vatScheme
    }

    "call to retrieveVatScheme return VatScheme from DB" in new Setup {
      when(mockRegistrationMongoRepository.retrieveVatScheme(RegistrationId("1"))).thenReturn(Future.successful(Some(vatScheme)))
      service.retrieveVatScheme(RegistrationId("1")) returnsRight vatScheme
    }

    "call to retrieveVatScheme return None from DB " in new Setup {
      when(mockRegistrationMongoRepository.retrieveVatScheme(RegistrationId("1"))).thenReturn(Future.successful(None))
      service.retrieveVatScheme(RegistrationId("1")) returnsLeft ResourceNotFound("1")
    }

    "return a new VatScheme response " in new Setup {
      val businessRegistrationSuccessResponse = Right(CurrentProfile("1", None, ""))

      when(mockRegistrationMongoRepository.retrieveVatScheme(RegistrationId("1"))).thenReturn(Future.successful(None))
      when(mockRegistrationMongoRepository.createNewVatScheme(RegistrationId("1"),internalid)).thenReturn(Future.successful(vatScheme))

      await(service.createNewRegistration(internalid).value) mustBe Right(vatScheme)
    }

    "error when creating VatScheme" in new Setup {
      val businessRegistrationSuccessResponse = Right(CurrentProfile("1", None, ""))
      val t = new Exception("Exception")

      when(mockRegistrationMongoRepository.retrieveVatScheme(RegistrationId("1"))).thenReturn(Future.successful(None))
      when(mockRegistrationMongoRepository.createNewVatScheme(RegistrationId("1"),internalid)).thenReturn(Future.failed(t))

      service.createNewRegistration(internalid) returnsLeft GenericError(t)
    }

    "error with the DB when creating VatScheme" in new Setup {
      val businessRegistrationSuccessResponse = Right(CurrentProfile("1", None, ""))
      val t = InsertFailed(RegistrationId("regId"), "VatScheme")

      when(mockRegistrationMongoRepository.retrieveVatScheme(RegistrationId("1"))).thenReturn(Future.successful(None))
      when(mockRegistrationMongoRepository.createNewVatScheme(RegistrationId("1"),internalid)).thenReturn(Future.failed(t))

      service.createNewRegistration(internalid) returnsLeft GenericDatabaseError(t, Some("regId"))
    }

    "call to business service return ForbiddenException response " in new Setup {
      service.createNewRegistration(internalid) returnsLeft ForbiddenAccess("forbidden")
    }

    "call to business service return NotFoundException response " in new Setup {
      service.createNewRegistration(internalid) returnsLeft ResourceNotFound("notfound")
    }

    "call to business service return ErrorResponse response " in new Setup {
      val t = new RuntimeException("Exception")

      service.createNewRegistration(internalid) returnsLeft GenericError(t)
    }

  }

  "call to deleteVatScheme" should {
    "return true" when {
      "the document has been deleted" in new Setup {
        val idMatcher: RegistrationId = RegistrationId(ArgumentMatchers.any())
        when(mockRegistrationMongoRepository.retrieveVatScheme(idMatcher)(ArgumentMatchers.any()))
          .thenReturn(Future.successful(Some(vatScheme)))

        when(mockRegistrationMongoRepository.deleteVatScheme(ArgumentMatchers.any())(ArgumentMatchers.any()))
          .thenReturn(Future.successful(true))

        await(service.deleteVatScheme("1", VatRegStatus.draft, VatRegStatus.rejected)) mustBe true
      }
    }

    "throw a MissingRegDoc exception" when {
      "no reg doc is found" in new Setup {
        val idMatcher: RegistrationId = RegistrationId(ArgumentMatchers.any())
        when(mockRegistrationMongoRepository.retrieveVatScheme(idMatcher)(ArgumentMatchers.any()))
          .thenReturn(Future.successful(None))

        intercept[MissingRegDocument](await(service.deleteVatScheme("1", VatRegStatus.draft, VatRegStatus.rejected)))
      }
    }

    "throw an InvalidSubmissionStatus exception" when {
      "the reg doc status is not valid for cancellation" in new Setup {
        val idMatcher: RegistrationId = RegistrationId(ArgumentMatchers.any())
        when(mockRegistrationMongoRepository.retrieveVatScheme(idMatcher)(ArgumentMatchers.any()))
          .thenReturn(Future.successful(Some(vatScheme.copy(status = VatRegStatus.submitted))))

        intercept[InvalidSubmissionStatus](await(service.deleteVatScheme("1", VatRegStatus.draft, VatRegStatus.rejected)))
      }
    }
  }


  "call to saveAcknowledgementReference" should {

    val vatScheme = VatScheme(RegistrationId("1"),internalid, None, None, None, status = VatRegStatus.draft)

    "return Success response " in new Setup {
      when(mockRegistrationMongoRepository.retrieveVatScheme(RegistrationId("1"))).thenReturn(Future.successful(Some(vatScheme)))
      when(mockRegistrationMongoRepository.updateByElement(RegistrationId("1"), AcknowledgementReferencePath, ackRefNumber))
        .thenReturn(Future.successful(ackRefNumber))
      service.saveAcknowledgementReference(RegistrationId("1"), ackRefNumber) returnsRight ackRefNumber
    }

    val vatSchemeWithAckRefNum = vatScheme.copy(acknowledgementReference = Some(ackRefNumber))
    "return Error response " in new Setup {
      when(mockRegistrationMongoRepository.retrieveVatScheme(RegistrationId("1"))).thenReturn(Future.successful(Some(vatSchemeWithAckRefNum)))
      service.saveAcknowledgementReference(RegistrationId("1"), ackRefNumber) returnsLeft
        AcknowledgementReferenceExists(s"""Registration ID 1 already has an acknowledgement reference of: $ackRefNumber""")
    }

    "return Error response for MissingVatSchemeDocument" in new Setup {
      val regId: RegistrationId = RegistrationId("regId")
      when(mockRegistrationMongoRepository.retrieveVatScheme(regId)).thenReturn(Future.successful(None))
      service.saveAcknowledgementReference(regId, ackRefNumber) returnsLeft ResourceNotFound(s"VatScheme ID: $regId missing")
    }
  }

  "call to retrieveAcknowledgementReference" should {

    "call to retrieveAcknowledgementReference return AcknowledgementReference from DB" in new Setup {
      val vatSchemeWithAckRefNum: VatScheme = vatScheme.copy(acknowledgementReference = Some(ackRefNumber))
      when(mockRegistrationMongoRepository.retrieveVatScheme(RegistrationId("1"))).thenReturn(Future.successful(Some(vatSchemeWithAckRefNum)))
      service.retrieveAcknowledgementReference(RegistrationId("1")) returnsRight ackRefNumber
    }

    "call to retrieveAcknowledgementReference return None from DB" in new Setup {
      when(mockRegistrationMongoRepository.retrieveVatScheme(RegistrationId("1"))).thenReturn(Future.successful(Some(vatScheme)))
      service.retrieveAcknowledgementReference(RegistrationId("1")) returnsLeft ResourceNotFound("AcknowledgementId")
    }
  }

  "call to getStatus" should {
    "return a correct JsValue" in new Setup {
      val expectedJson: JsValue = Json.parse(
        """
          |{
          | "status":"draft",
          | "cancelURL":"/test/uri"
          |}
        """.stripMargin)

      when(mockRegistrationMongoRepository.retrieveVatScheme(RegistrationId("1"))).thenReturn(Future.successful(Some(vatScheme)))

      await(service.getStatus(RegistrationId("1"))) mustBe expectedJson
    }

    "return a correct JsValue with ackRef" in new Setup {
      val vatSchemeWithAckRefNum: VatScheme = vatScheme.copy(acknowledgementReference = Some(ackRefNumber))
      val expectedJson: JsValue = Json.parse(
        s"""
           |{
           |  "status":"draft",
           |  "ackRef":"BRPY000000000001",
           |  "cancelURL":"/test/uri"
           |}
          """.stripMargin)

      when(mockRegistrationMongoRepository.retrieveVatScheme(RegistrationId("1"))).thenReturn(Future.successful(Some(vatSchemeWithAckRefNum)))
      await(service.getStatus(RegistrationId("1"))) mustBe expectedJson
    }
  }

  "call to clearDownDocument" should {
    "pass" when {
      "given a transactionid" in new Setup {
        when(mockRegistrationMongoRepository.clearDownDocument(ArgumentMatchers.eq("testTransID"))(any())).thenReturn(Future.successful(true))
        await(service.clearDownDocument("testTransID")) mustBe true
      }
    }
  }

  "call to getBlockFromEligibilityData" should {
    val thresholdPreviousThirtyDays = LocalDate.of(2017, 5, 23)
    val thresholdInTwelveMonths = LocalDate.of(2017, 7, 16)

    "return nothing if nothing in EligibilityData" in new Setup {
      when(mockRegistrationMongoRepository.getEligibilityData(any())(any())).thenReturn(Future.successful(None))

      await(service.getBlockFromEligibilityData[Threshold]("regId")) mustBe None
    }

    "return correct Threshold model" in new Setup {
      val questions = Seq(
        Json.obj("questionId" -> "thresholdPreviousThirtyDays-optionalData", "question" -> "Some Question 12", "answer" -> "Some Answer 12",
          "answerValue" -> thresholdPreviousThirtyDays.toString),
        Json.obj("questionId" -> "thresholdInTwelveMonths-optionalData", "question" -> "Some Question 12", "answer" -> "Some Answer 12",
          "answerValue" -> thresholdInTwelveMonths.toString)
      )
      val section: JsObject = Json.obj("title" -> "test TITLE 1", "data" -> JsArray(questions))
      val jsonThresholdInEligibilityDataFormat: JsObject = Json.obj("sections" -> section)

      val expected: Threshold = Threshold(
        mandatoryRegistration = true,
        thresholdPreviousThirtyDays = Some(thresholdPreviousThirtyDays),
        thresholdInTwelveMonths = Some(thresholdInTwelveMonths)
      )

      when(mockRegistrationMongoRepository.getEligibilityData(any())(any())).thenReturn(Future.successful(Some(jsonThresholdInEligibilityDataFormat)))

      implicit val read: Reads[Threshold] = Threshold.eligibilityDataJsonReads

      await(service.getBlockFromEligibilityData[Threshold]("regId")) mustBe Some(expected)
    }

    "return correct TurnoverEstimates model when turnover estimate is provided with a number" in new Setup {
      val questions = Seq(
        Json.obj("questionId" -> "turnoverEstimate-value", "question" -> "Some Question 11", "answer" -> "£10,001", "answerValue" -> 10001)
      )
      val section: JsObject = Json.obj("title" -> "test TITLE 1", "data" -> JsArray(questions))
      val jsonTurnoverEstimatesDataFormat: JsObject = Json.obj("sections" -> section)

      val expected: TurnoverEstimates = TurnoverEstimates(turnoverEstimate = 10001)

      when(mockRegistrationMongoRepository.getEligibilityData(any())(any())).thenReturn(Future.successful(Some(jsonTurnoverEstimatesDataFormat)))

      implicit val read: Reads[TurnoverEstimates] = TurnoverEstimates.eligibilityDataJsonReads

      await(service.getBlockFromEligibilityData[TurnoverEstimates]("regId")) mustBe Some(expected)
    }

    "return error when json is not correct to return a model" in new Setup {
      val questions = Seq(
        Json.obj("questionId" -> "fooNotaRealQuestionID", "question" -> "Some Question 11", "answer" -> "Some Answer 11", "answerValue" -> 2024)
      )
      val section: JsObject = Json.obj("title" -> "test TITLE 1", "data" -> JsArray(questions))
      val jsonThresholdDataFormat: JsObject = Json.obj("sections" -> section)

      when(mockRegistrationMongoRepository.getEligibilityData(any())(any())).thenReturn(Future.successful(Some(jsonThresholdDataFormat)))

      an[InvalidEligibilityDataToConvertModel] mustBe thrownBy(await(service.getBlockFromEligibilityData[Threshold]("regId")))
    }
  }
}

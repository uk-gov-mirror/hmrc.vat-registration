/*
 * Copyright 2017 HM Revenue & Customs
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

import cats.instances.FutureInstances
import cats.syntax.ApplicativeSyntax
import common.{RegistrationId, TransactionId}
import common.exceptions._
import enums.VatRegStatus
import fixtures.VatRegistrationFixture
import helpers.VatRegSpec
import models.api._
import models.submission.DESSubmission
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito._
import play.api.libs.json.{JsValue, Json}
import play.api.test.Helpers.{OK, NOT_FOUND}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, NotFoundException}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class SubmissionServiceSpec extends VatRegSpec with VatRegistrationFixture with ApplicativeSyntax with FutureInstances {

  trait Setup {
    val service = new SubmissionService(
      mockSequenceRepository,
      mockVatRegistrationService,
      mockRegistrationRepository,
      mockCompanyRegConnector,
      mockDesConnector,
      mockIIConnector
    )
  }

  implicit val hc = HeaderCarrier()

  "call to getAcknowledgementReference" should {

    val vatScheme = VatScheme(RegistrationId("1"), None, None, None, status = VatRegStatus.draft)

    "return Success response " in new Setup {
      when(mockVatRegistrationService.retrieveAcknowledgementReference(regId)).
        thenReturn(ServiceMocks.serviceResult(ackRefNumber))

      service.getAcknowledgementReference(regId) returnsRight ackRefNumber
    }

    "return ResourceNotFound response " in new Setup {
      val resourceNotFound = ResourceNotFound("Resource Not Found for regId 1")
      when(mockVatRegistrationService.retrieveAcknowledgementReference(RegistrationId(anyString()))(ArgumentMatchers.any()))
        .thenReturn(ServiceMocks.serviceError[String](resourceNotFound))

      service.getAcknowledgementReference(regId) returnsLeft resourceNotFound
    }
  }

  "ensureAcknowledgementReference" should {
    val vatScheme = VatScheme(RegistrationId("1"), None, None, None, status = VatRegStatus.draft, acknowledgementReference = Some("testref"))
    val sequenceNo = 1
    val formatedRefNumber = f"BRVT$sequenceNo%011d"

    "throw an exception if the document is not available" in new Setup {
      when(mockRegistrationRepository.retrieveVatScheme(RegistrationId(anyString()))(ArgumentMatchers.any()))
        .thenReturn(None)

      intercept[MissingRegDocument](await(service.ensureAcknowledgementReference(regId)))
    }

    "get the acknowledgement references if they are available" in new Setup {
      when(mockRegistrationRepository.retrieveVatScheme(RegistrationId(anyString()))(ArgumentMatchers.any()))
        .thenReturn(Some(vatScheme))

      await(service.ensureAcknowledgementReference(regId)) shouldBe "testref"
    }

    "generate acknowledgment reference if it does not exist" in new Setup {
      when(mockRegistrationRepository.retrieveVatScheme(RegistrationId(anyString()))(ArgumentMatchers.any()))
        .thenReturn(Some(vatScheme.copy(status = VatRegStatus.draft, acknowledgementReference = None)))
      when(mockSequenceRepository.getNext(ArgumentMatchers.eq("AcknowledgementID"))(ArgumentMatchers.any())).thenReturn(sequenceNo.pure)
      when(mockRegistrationRepository.prepareRegistrationSubmission(RegistrationId(anyString()), ArgumentMatchers.any())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(true))

      await(service.ensureAcknowledgementReference(regId)) shouldBe formatedRefNumber
    }
  }

  "getValidDocumentStatus" should {
    val vatScheme = VatScheme(RegistrationId("1"), None, None, None, status = VatRegStatus.draft)

    "throw an exception if the document is not available" in new Setup {
      when(mockRegistrationRepository.retrieveVatScheme(RegistrationId(anyString()))(ArgumentMatchers.any()))
        .thenReturn(None)

      intercept[MissingRegDocument](await(service.getValidDocumentStatus(regId)))
    }

    "throw an exception if the document is not locked or draft" in new Setup {
      when(mockRegistrationRepository.retrieveVatScheme(RegistrationId(anyString()))(ArgumentMatchers.any()))
        .thenReturn(Some(vatScheme.copy(status = VatRegStatus.cancelled)))

      intercept[InvalidSubmissionStatus](await(service.getValidDocumentStatus(regId)))
    }

    "return the status as being draft" in new Setup {
      when(mockRegistrationRepository.retrieveVatScheme(RegistrationId(anyString()))(ArgumentMatchers.any()))
        .thenReturn(Some(vatScheme))

      await(service.getValidDocumentStatus(regId)) shouldBe "draft"
    }
  }

  "fetchCompanyRegistrationTransactionID" should {
    "on a successful ok response with transID, return the transID" in new Setup {
      val testJson = Json.parse(
        """
          |{
          | "confirmationReferences": {
          |   "transaction-id" : "transID"
          | }
          |}
        """.stripMargin
      )

      val okResponse = new HttpResponse {
        override def status: Int = OK
        override def json: JsValue = testJson
      }

      when(mockCompanyRegConnector.fetchCompanyRegistrationDocument(RegistrationId(anyString()))(ArgumentMatchers.any()))
        .thenReturn(Future.successful(okResponse))

      await(service.fetchCompanyRegistrationTransactionID(regId)) shouldBe "transID"
    }

    "on a successful ok response without transID, throw a NoTransactionId exception" in new Setup {
      val testJson = Json.parse(
        """
          |{
          | "confirmationReferences": {
          |   "test" : "test"
          | }
          |}
        """.stripMargin
      )

      val okResponse = new HttpResponse {
        override def status: Int = OK
        override def json: JsValue = testJson
      }

      when(mockCompanyRegConnector.fetchCompanyRegistrationDocument(RegistrationId(anyString()))(ArgumentMatchers.any()))
        .thenReturn(Future.successful(okResponse))

      intercept[NoTransactionId](await(service.fetchCompanyRegistrationTransactionID(regId)))
    }
  }

  "getCompanyName" should {
    "return the company name if it exists" in new Setup {
      val testJson = Json.parse(
        """
          |{
          | "company_name": "companyname"
          |}
        """.stripMargin
      )

      val okResponse = new HttpResponse {
        override def status: Int = OK
        override def json: JsValue = testJson
      }

      when(mockIIConnector.getCompanyName(RegistrationId(anyString()), TransactionId(anyString()))(ArgumentMatchers.any()))
        .thenReturn(Future.successful(okResponse))

      await(service.getCompanyName(regId, txId)) shouldBe "companyname"
    }

    "throw an exception if it does not" in new Setup {
      when(mockIIConnector.getCompanyName(RegistrationId(anyString()), TransactionId(anyString()))(ArgumentMatchers.any()))
        .thenReturn(Future.failed(new NotFoundException("not found")))

      intercept[NotFoundException](await(service.getCompanyName(regId, txId)))
    }

    "fails to convert the company_name to json" in new Setup {
      val testJson = Json.parse(
        """
          |{
          |}
        """.stripMargin
      )

      val okResponse = new HttpResponse {
        override def status: Int = OK
        override def json: JsValue = testJson
      }

      when(mockIIConnector.getCompanyName(RegistrationId(anyString()), TransactionId(anyString()))(ArgumentMatchers.any()))
        .thenReturn(Future.successful(okResponse))

      intercept[NoCompanyName](await(service.getCompanyName(regId, txId)))
    }
  }

  "getIncorporationUpdate" should {
    "return the incorporation status if a transaction ID is provided" in new Setup {
      val incorpstatus = incorporationStatus()

      when(mockIIConnector.retrieveIncorporationStatus(TransactionId(anyString()), ArgumentMatchers.any(), ArgumentMatchers.any())
        (ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(Some(incorpstatus)))

      await(service.registerForInterest("transID")) shouldBe incorpstatus
    }

    "throw a NoIncorpUpdate exception if it could not retrieve one" in new Setup {
      when(mockIIConnector.retrieveIncorporationStatus(TransactionId(anyString()), ArgumentMatchers.any(), ArgumentMatchers.any())
      (ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(None)

      intercept[NoIncorpUpdate](await(service.registerForInterest("transID")))
    }
  }

  "getIncorpDate" should {
    val incorpStatus = incorporationStatus()
    "return the incorp date status if it exists" in new Setup {
      service.getIncorpDate(incorpStatus) shouldBe incorpStatus.statusEvent.incorporationDate.get
    }

    "throw a NoIncorpDate exception if it could not retrieve one" in new Setup {
      val incorpWithoutDate = incorpStatus.copy(
        statusEvent = incorpStatus.statusEvent.copy(incorporationDate = None)
      )
      intercept[NoIncorpDate](service.getIncorpDate(incorpWithoutDate))
    }
  }

  "Calling buildDesSubmission" should {

    val vatScheme = VatScheme(RegistrationId("1"), Some(tradingDetails), None, None, status = VatRegStatus.draft)
    val vatSchemeNoTradingDetails = VatScheme(RegistrationId("1"), None, None, None, status = VatRegStatus.draft)
    val vatChoiceNoStartDate = vatChoice.copy(vatStartDate = VatStartDate(selection = "SPECIFIC_DATE", startDate = None))
    val tradingDetailsNoStartDate = tradingDetails.copy(vatChoice = vatChoiceNoStartDate)
    val vatSchemeNoStartDate = VatScheme(RegistrationId("1"), Some(tradingDetailsNoStartDate), None, None, status = VatRegStatus.draft)

    val desSubmission = DESSubmission("ackRef", "companyName", date, date)

    "successfully create a des submission" in new Setup {
      when(mockRegistrationRepository.retrieveVatScheme(RegistrationId(anyString()))(ArgumentMatchers.any()))
        .thenReturn(Future.successful(Some(vatScheme)))

      await(service.buildDesSubmission(regId, "ackRef", "companyName", date)) shouldBe desSubmission
    }

    "throw a MissingRegDocument exception when there is no registration in mongo" in new Setup {
      when(mockRegistrationRepository.retrieveVatScheme(RegistrationId(anyString()))(ArgumentMatchers.any()))
        .thenReturn(Future.successful(None))

      intercept[MissingRegDocument](await(service.buildDesSubmission(regId, "ackRef", "companyName", LocalDate.now())))
    }

    "throw a NoTradingDetails exception when the vat scheme doesn't contain trading details" in new Setup {
      when(mockRegistrationRepository.retrieveVatScheme(RegistrationId(anyString()))(ArgumentMatchers.any()))
        .thenReturn(Future.successful(Some(vatSchemeNoTradingDetails)))

      intercept[NoTradingDetails](await(service.buildDesSubmission(regId, "ackRef", "companyName", LocalDate.now())))
    }

    "throw a NoVatStartDate exception when the vat schemes trading details doesn't contain a vat start date" in new Setup {
      when(mockRegistrationRepository.retrieveVatScheme(RegistrationId(anyString()))(ArgumentMatchers.any()))
        .thenReturn(Future.successful(Some(vatSchemeNoStartDate)))

      intercept[NoVatStartDate](await(service.buildDesSubmission(regId, "ackRef", "companyName", LocalDate.now())))
    }
  }
}

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

import cats.instances.FutureInstances
import cats.syntax.ApplicativeSyntax
import common.TransactionId
import common.exceptions._
import enums.VatRegStatus
import fixtures.VatRegistrationFixture
import helpers.VatRegSpec
import models.api._
import models.submission.DESSubmission
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.{any, anyString}
import org.mockito.Mockito._
import play.api.libs.json.Json
import play.api.test.Helpers._
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class SubmissionServiceSpec extends VatRegSpec with VatRegistrationFixture with ApplicativeSyntax with FutureInstances {

  class Setup {
    val service: SubmissionService = new SubmissionService(
      sequenceMongoRepository = mockSequenceRepository,
      vatRegistrationService = mockVatRegistrationService,
      registrationRepository = mockRegistrationMongoRepository,
      desConnector = mockDesConnector
    )
  }

  implicit val hc: HeaderCarrier = HeaderCarrier()

  "submitVatRegistration" should {
    val transactionIdJson = Json.obj("confirmationReferences" -> Json.obj("transaction-id" -> "foo"))

    "successfully return a future string when mockSubmission = false" in new Setup {
      when(mockRegistrationMongoRepository.retrieveVatScheme(anyString())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(Some(testVatScheme.copy(returns = Some(Returns(true, "", Some("foo"), StartDate(Some(LocalDate.now))))))))
      when(mockSequenceRepository.getNext(any())(any())).thenReturn(Future.successful(100))
      when(mockRegistrationMongoRepository.prepareRegistrationSubmission(anyString(), any(), any())(any())).thenReturn(Future.successful(true))
      when(mockRegistrationMongoRepository.saveTransId(any(), anyString())(any())).thenReturn(Future.successful("transID"))
      when(mockDesConnector.submitToDES(any[VatSubmission], any())(any())).thenReturn(Future.successful(HttpResponse(200)))
      when(mockRegistrationMongoRepository.finishRegistrationSubmission(anyString(), any())(any())).thenReturn(Future.successful(VatRegStatus.submitted))

      await(service.submitVatRegistration(testRegId)) mustBe "BRVT00000000100"

    }
    "successfully submit to des using mockSubmission = true" in new Setup {
      when(mockRegistrationMongoRepository.retrieveVatScheme(anyString())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(Some(testVatScheme.copy(returns = Some(Returns(true, "", Some("foo"), StartDate(Some(LocalDate.now))))))))
      when(mockSequenceRepository.getNext(any())(any())).thenReturn(Future.successful(100))
      when(mockRegistrationMongoRepository.prepareRegistrationSubmission(anyString(), any(), any())(any())).thenReturn(Future.successful(true))
      when(mockRegistrationMongoRepository.saveTransId(any(), anyString())(any())).thenReturn(Future.successful("transID"))
      when(mockDesConnector.submitToDES(any[VatSubmission], any())(any())).thenReturn(Future.successful(HttpResponse(200)))
      when(mockRegistrationMongoRepository.finishRegistrationSubmission(anyString(), any())(any())).thenReturn(Future.successful(VatRegStatus.submitted))

      await(service.submitVatRegistration(testRegId)) mustBe "BRVT00000000100"
    }
  }


  "call to getAcknowledgementReference" should {

    val vatScheme = VatScheme(testRegId, internalId = testInternalid, None, None, None, status = VatRegStatus.draft)

    "return Success response " in new Setup {
      when(mockVatRegistrationService.retrieveAcknowledgementReference(testRegId)).
        thenReturn(ServiceMocks.serviceResult(testAckReference))

      service.getAcknowledgementReference(testRegId) returnsRight testAckReference
    }

    "return ResourceNotFound response " in new Setup {
      val resourceNotFound: ResourceNotFound = ResourceNotFound("Resource Not Found for regId 1")
      when(mockVatRegistrationService.retrieveAcknowledgementReference(anyString())(ArgumentMatchers.any()))
        .thenReturn(ServiceMocks.serviceError[String](resourceNotFound))

      service.getAcknowledgementReference(testRegId) returnsLeft resourceNotFound
    }
  }

  "ensureAcknowledgementReference" should {
    val vatScheme = VatScheme(testRegId, testInternalid, None, None, None, status = VatRegStatus.draft, acknowledgementReference = Some("testref"))
    val sequenceNo = 1
    val formattedRefNumber = f"BRVT$sequenceNo%011d"

    "throw an exception if the document is not available" in new Setup {
      when(mockRegistrationMongoRepository.retrieveVatScheme(anyString())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(None))

      intercept[MissingRegDocument](await(service.ensureAcknowledgementReference(testRegId, VatRegStatus.draft)))
    }

    "get the acknowledgement references if they are available" in new Setup {
      when(mockRegistrationMongoRepository.retrieveVatScheme(anyString())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(Some(vatScheme)))

      await(service.ensureAcknowledgementReference(testRegId, VatRegStatus.draft)) mustBe "testref"
    }

    "generate acknowledgment reference if it does not exist" in new Setup {
      when(mockRegistrationMongoRepository.retrieveVatScheme(anyString())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(Some(vatScheme.copy(status = VatRegStatus.draft, acknowledgementReference = None))))
      when(mockSequenceRepository.getNext(ArgumentMatchers.eq("AcknowledgementID"))(ArgumentMatchers.any())).thenReturn(sequenceNo.pure)
      when(mockRegistrationMongoRepository.prepareRegistrationSubmission(anyString(), ArgumentMatchers.any(),
        ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(true))

      await(service.ensureAcknowledgementReference(testRegId, VatRegStatus.draft)) mustBe formattedRefNumber
    }
  }

  "getValidDocumentStatus" should {
    val vatScheme = VatScheme(testRegId, testInternalid, None, None, None, status = VatRegStatus.draft)

    "throw an exception if the document is not available" in new Setup {
      when(mockRegistrationMongoRepository.retrieveVatScheme(ArgumentMatchers.eq(testRegId))(ArgumentMatchers.any()))
        .thenReturn(Future.successful(None))

      intercept[MissingRegDocument](await(service.getValidDocumentStatus(testRegId)))
    }

    "throw an exception if the document is not locked or draft" in new Setup {
      when(mockRegistrationMongoRepository.retrieveVatScheme(ArgumentMatchers.eq(testRegId))(ArgumentMatchers.any()))
        .thenReturn(Future.successful(Some(vatScheme.copy(status = VatRegStatus.cancelled))))

      intercept[InvalidSubmissionStatus](await(service.getValidDocumentStatus(testRegId)))
    }

    "return the status as being draft" in new Setup {
      when(mockRegistrationMongoRepository.retrieveVatScheme(ArgumentMatchers.eq(testRegId))(ArgumentMatchers.any()))
        .thenReturn(Future.successful(Some(vatScheme)))

      await(service.getValidDocumentStatus(testRegId)) mustBe VatRegStatus.draft
    }
  }

  "updateSubmissionStatus" should {
    "update the submission to submitted if there is an incorp date" in new Setup {
      when(mockRegistrationMongoRepository.finishRegistrationSubmission(ArgumentMatchers.eq(testRegId), ArgumentMatchers.any())
      (ArgumentMatchers.any())).thenReturn(Future.successful(VatRegStatus.submitted))

      await(service.updateSubmissionStatus(testRegId)) mustBe VatRegStatus.submitted
    }

    "update the submission to held if there is no incorp date" in new Setup {
      when(mockRegistrationMongoRepository.finishRegistrationSubmission(ArgumentMatchers.eq(testRegId), ArgumentMatchers.any())
      (ArgumentMatchers.any())).thenReturn(Future.successful(VatRegStatus.held))

      await(service.updateSubmissionStatus(testRegId)) mustBe VatRegStatus.held
    }
  }

  "Calling buildDesSubmission" should {

    val schemeReturns = Returns(true, "monthly", None, StartDate(date = Some(testDate)))
    val vatScheme = VatScheme(testRegId, testInternalid, Some(TransactionId("1")), returns = Some(schemeReturns), status = VatRegStatus.draft)
    val vatSchemeNoTradingDetails = VatScheme(testRegId, testInternalid, None, None, None, status = VatRegStatus.draft)
    val vatSchemeNoStartDate = VatScheme(testRegId, testInternalid, Some(TransactionId("1")), None, None, status = VatRegStatus.draft)
    val fullDESSubmission = DESSubmission("ackRef", Some(testDate))
    val partialDESSubmission = DESSubmission("ackRef", Some(testDate))

    val someNow = Some(LocalDate.now())

    "successfully create a full DES submission" in new Setup {
      when(mockRegistrationMongoRepository.retrieveVatScheme(ArgumentMatchers.eq(testRegId))(ArgumentMatchers.any()))
        .thenReturn(Future.successful(Some(vatScheme)))

      await(service.buildDesSubmission(testRegId, "ackRef")) mustBe fullDESSubmission
    }

    "successfully create a partial DES submission" in new Setup {
      when(mockRegistrationMongoRepository.retrieveVatScheme(anyString())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(Some(vatScheme)))

      await(service.buildDesSubmission(testRegId, "ackRef")) mustBe partialDESSubmission
    }

    "throw a MissingRegDocument exception when there is no registration in mongo" in new Setup {
      when(mockRegistrationMongoRepository.retrieveVatScheme(anyString())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(None))
      intercept[MissingRegDocument](await(service.buildDesSubmission(testRegId, "ackRef")))
    }

    "throw a NoRetuens exception when the vat scheme doesn't contain returns" in new Setup {
      when(mockRegistrationMongoRepository.retrieveVatScheme(anyString())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(Some(vatSchemeNoTradingDetails)))

      intercept[NoReturns](await(service.buildDesSubmission(testRegId, "ackRef")))
    }
  }

}

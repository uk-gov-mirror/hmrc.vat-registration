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

import common.RegistrationId
import common.exceptions._
import enums.VatRegStatus
import fixtures.VatRegistrationFixture
import helpers.VatRegSpec
import models._
import models.api._
import models.external.CurrentProfile
import org.mockito.Matchers
import org.mockito.Matchers.any
import org.mockito.Mockito._
import play.api.libs.json.Json
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.Future

class VatRegistrationServiceSpec extends VatRegSpec with VatRegistrationFixture {

  trait Setup {
    val service = new VatRegistrationService(mockBusRegConnector, mockRegistrationRepository)
  }

  implicit val hc = HeaderCarrier()

  "createNewRegistration" should {

    val vatScheme = VatScheme(RegistrationId("1"), None, None, None, status = VatRegStatus.draft)

    "return a existing VatScheme response " in new Setup {
      val businessRegistrationSuccessResponse = Right(CurrentProfile("1", None, ""))

      when(mockBusRegConnector.retrieveCurrentProfile(any(), any())).thenReturn(businessRegistrationSuccessResponse)
      when(mockRegistrationRepository.retrieveVatScheme(RegistrationId("1"))).thenReturn(Some(vatScheme))

      service.createNewRegistration() returnsRight vatScheme
    }

    "call to retrieveVatScheme return VatScheme from DB" in new Setup {
      when(mockRegistrationRepository.retrieveVatScheme(RegistrationId("1"))).thenReturn(Future.successful(Some(vatScheme)))
      service.retrieveVatScheme(RegistrationId("1")) returnsRight vatScheme
    }

    "call to retrieveVatScheme return None from DB " in new Setup {
      when(mockRegistrationRepository.retrieveVatScheme(RegistrationId("1"))).thenReturn(Future.successful(None))
      service.retrieveVatScheme(RegistrationId("1")) returnsLeft ResourceNotFound("1")
    }

    "return a new VatScheme response " in new Setup {
      val businessRegistrationSuccessResponse = Right(CurrentProfile("1", None, ""))

      when(mockBusRegConnector.retrieveCurrentProfile(any(), any())).thenReturn(businessRegistrationSuccessResponse)
      when(mockRegistrationRepository.retrieveVatScheme(RegistrationId("1"))).thenReturn(None)
      when(mockRegistrationRepository.createNewVatScheme(RegistrationId("1"))).thenReturn(vatScheme)

      service.createNewRegistration() returnsRight vatScheme
    }

    "error when creating VatScheme" in new Setup {
      val businessRegistrationSuccessResponse = Right(CurrentProfile("1", None, ""))
      val t = new Exception("Exception")

      when(mockBusRegConnector.retrieveCurrentProfile(any(), any())).thenReturn(businessRegistrationSuccessResponse)
      when(mockRegistrationRepository.retrieveVatScheme(RegistrationId("1"))).thenReturn(None)
      when(mockRegistrationRepository.createNewVatScheme(RegistrationId("1"))).thenReturn(Future.failed(t))

      service.createNewRegistration() returnsLeft GenericError(t)
    }

    "error with the DB when creating VatScheme" in new Setup {
      val businessRegistrationSuccessResponse = Right(CurrentProfile("1", None, ""))
      val t = InsertFailed(RegistrationId("regId"), "VatScheme")

      when(mockBusRegConnector.retrieveCurrentProfile(any(), any())).thenReturn(businessRegistrationSuccessResponse)
      when(mockRegistrationRepository.retrieveVatScheme(RegistrationId("1"))).thenReturn(None)
      when(mockRegistrationRepository.createNewVatScheme(RegistrationId("1"))).thenReturn(Future.failed(t))

      service.createNewRegistration() returnsLeft GenericDatabaseError(t, Some("regId"))
    }

    "call to business service return ForbiddenException response " in new Setup {
      when(mockBusRegConnector.retrieveCurrentProfile(any(), any())).thenReturn(Left(ForbiddenAccess("forbidden")))

      service.createNewRegistration() returnsLeft ForbiddenAccess("forbidden")
    }

    "call to business service return NotFoundException response " in new Setup {
      when(mockBusRegConnector.retrieveCurrentProfile(any(), any())).thenReturn(Left(ResourceNotFound("notfound")))

      service.createNewRegistration() returnsLeft ResourceNotFound("notfound")
    }

    "call to business service return ErrorResponse response " in new Setup {
      val t = new RuntimeException("Exception")
      when(mockBusRegConnector.retrieveCurrentProfile(any(), any())).thenReturn(Left(GenericError(t)))

      service.createNewRegistration() returnsLeft GenericError(t)
    }

  }

  "call to updateLogicalGroup" should {

    val tradingDetails = VatTradingDetails(
      vatChoice = vatChoice,
      tradingName = TradingName(
        selection = true,
        tradingName = Some("some-trader-name")),
      euTrading = VatEuTrading(selection = true, eoriApplication = Some(true)))

    "return Success response " in new Setup {
      when(mockRegistrationRepository.updateLogicalGroup(RegistrationId("1"), tradingDetails)).thenReturn(tradingDetails)
      service.updateLogicalGroup(RegistrationId("1"), tradingDetails) returnsRight tradingDetails
    }

    "return Error response " in new Setup {
      val t = new RuntimeException("Exception")
      when(mockRegistrationRepository.updateLogicalGroup(RegistrationId("1"), tradingDetails)).thenReturn(Future.failed(t))

      service.updateLogicalGroup(RegistrationId("1"), tradingDetails) returnsLeft GenericError(t)
    }

    "return Error response for MissingRegDocument" in new Setup {
      val regId = RegistrationId("regId")
      val t = MissingRegDocument(regId)
      when(mockRegistrationRepository.updateLogicalGroup(regId, tradingDetails)).thenReturn(Future.failed(t))

      service.updateLogicalGroup(regId, tradingDetails) returnsLeft ResourceNotFound(s"No registration found for registration ID: $regId")
    }
  }

  "call to deleteVatScheme" should {

    "return Success response " in new Setup {
      when(mockRegistrationRepository.deleteVatScheme(RegistrationId("1"))).thenReturn(Future.successful(true))
      service.deleteVatScheme(RegistrationId("1")) returnsRight true
    }


    "return Error response " in new Setup {
      val t = new RuntimeException("Exception")
      when(mockRegistrationRepository.deleteVatScheme(RegistrationId("1"))).thenReturn(Future.failed(t))
      service.deleteVatScheme(RegistrationId("1")) returnsLeft GenericError(t)
    }

    "return Error response for MissingRegDocument" in new Setup {
      val regId = RegistrationId("regId")
      val t = MissingRegDocument(regId)
      when(mockRegistrationRepository.deleteVatScheme(RegistrationId("1"))).thenReturn(Future.failed(t))
      service.deleteVatScheme(RegistrationId("1")) returnsLeft ResourceNotFound(s"No registration found for registration ID: $regId")
    }
  }


  "call to saveAcknowledgementReference" should {

    val vatScheme = VatScheme(RegistrationId("1"), None, None, None, status = VatRegStatus.draft)

    "return Success response " in new Setup {
      when(mockRegistrationRepository.retrieveVatScheme(RegistrationId("1"))).thenReturn(Some(vatScheme))
      when(mockRegistrationRepository.updateByElement(RegistrationId("1"), AcknowledgementReferencePath, ackRefNumber)).thenReturn(ackRefNumber)
      service.saveAcknowledgementReference(RegistrationId("1"), ackRefNumber) returnsRight ackRefNumber
    }

    val vatSchemeWithAckRefNum = vatScheme.copy(acknowledgementReference = Some(ackRefNumber))
    "return Error response " in new Setup {
      when(mockRegistrationRepository.retrieveVatScheme(RegistrationId("1"))).thenReturn(Some(vatSchemeWithAckRefNum))
      service.saveAcknowledgementReference(RegistrationId("1"), ackRefNumber) returnsLeft
        AcknowledgementReferenceExists(s"""Registration ID 1 already has an acknowledgement reference of: $ackRefNumber""")
    }

    "return Error response for MissingVatSchemeDocument" in new Setup {
      val regId = RegistrationId("regId")
      when(mockRegistrationRepository.retrieveVatScheme(regId)).thenReturn(Future.successful(None))
      service.saveAcknowledgementReference(regId, ackRefNumber) returnsLeft ResourceNotFound(s"VatScheme ID: $regId missing")
    }
  }


  "call to deleteByElement" should {
    "return Success response " in new Setup {
      when(mockRegistrationRepository.deleteByElement(RegistrationId("1"), VatBankAccountPath)).thenReturn(Future.successful(true))
      service.deleteByElement(RegistrationId("1"), VatBankAccountPath) returnsRight true
    }

    "return Error response " in new Setup {
      val t = new RuntimeException("Exception")
      when(mockRegistrationRepository.deleteByElement(RegistrationId("1"), VatBankAccountPath)).thenReturn(Future.failed(t))
      service.deleteByElement(RegistrationId("1"), VatBankAccountPath) returnsLeft GenericError(t)
    }
  }

  "call to retrieveAcknowledgementReference" should {

    "call to retrieveAcknowledgementReference return AcknowledgementReference from DB" in new Setup {
      val vatSchemeWithAckRefNum = vatScheme.copy(acknowledgementReference = Some(ackRefNumber))
      when(mockRegistrationRepository.retrieveVatScheme(RegistrationId("1"))).thenReturn(Future.successful(Some(vatSchemeWithAckRefNum)))
      service.retrieveAcknowledgementReference(RegistrationId("1")) returnsRight ackRefNumber
    }

    "call to retrieveAcknowledgementReference return None from DB" in new Setup {
      when(mockRegistrationRepository.retrieveVatScheme(RegistrationId("1"))).thenReturn(Future.successful(Some(vatScheme)))
      service.retrieveAcknowledgementReference(RegistrationId("1")) returnsLeft ResourceNotFound("AcknowledgementId")
    }
  }


  "call to getStatus" should {
    "return a correct JsValue" in new Setup {
      val expectedJson = Json.parse(
        """
          | {
          |   "status": "draft"
          | }
        """.stripMargin)

      when(mockRegistrationRepository.retrieveVatScheme(RegistrationId("1"))).thenReturn(Future.successful(Some(vatScheme)))
      service.getStatus(RegistrationId("1")) returnsRight expectedJson
    }

    "return a correct JsValue with ackRef" in new Setup {
      val vatSchemeWithAckRefNum = vatScheme.copy(acknowledgementReference = Some(ackRefNumber))
      val expectedJson = Json.parse(
        s"""
          | {
          |   "status": "draft",
          |   "ackRef": "$ackRefNumber"
          | }
        """.stripMargin)

      when(mockRegistrationRepository.retrieveVatScheme(RegistrationId("1"))).thenReturn(Future.successful(Some(vatSchemeWithAckRefNum)))
      service.getStatus(RegistrationId("1")) returnsRight expectedJson
    }

    "return a ResourceNotFound" in new Setup {
      when(mockRegistrationRepository.retrieveVatScheme(RegistrationId("1"))).thenReturn(Future.successful(None))
      service.getStatus(RegistrationId("1")) returnsLeft ResourceNotFound(
        "No registration found for registration ID: 1")
    }
  }
  "updateIVStatus" should {
    "return a boolean" when {
      "the user IV status has been updated" in new Setup {
        when(mockRegistrationRepository.updateIVStatus(Matchers.any(), Matchers.any()))
          .thenReturn(Future.successful(true))

        val result = await(service.updateIVStatus("testRegId", true))
        result shouldBe true
      }
    }

    "throw an updated failed" in new Setup {
      when(mockRegistrationRepository.updateIVStatus(Matchers.any(), Matchers.any()))
        .thenReturn(Future.failed(UpdateFailed(RegistrationId("testRegId"), "testModel")))

      intercept[UpdateFailed](await(service.updateIVStatus("testRegId", true)))

    }
  }
}

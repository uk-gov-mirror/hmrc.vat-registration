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

import common.Identifiers.RegistrationId
import common.exceptions._
import connectors._
import helpers.VatRegSpec
import models._
import models.external.CurrentProfile
import org.mockito.Matchers
import org.mockito.Matchers.any
import org.mockito.Mockito._
import repositories.RegistrationRepository
import repositories.test.TestOnlyRepository
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.Future

class VatRegistrationServiceSpec extends VatRegSpec {

  val mockBusRegConnector = mock[BusinessRegistrationConnector]
  val mockRegistrationRepository = mock[RegistrationRepository]
  val mockTestOnlyRepo = mock[TestOnlyRepository]
  val date = LocalDate.of(2017, 1, 1)
  val vatChoice: VatChoice = VatChoice(date, "")

  trait Setup {
    val service = new VatRegistrationService(mockBusRegConnector, mockRegistrationRepository)
  }

  implicit val hc = HeaderCarrier()

  "createNewRegistration" should {

    val vatScheme = VatScheme("1", None, None, None)

    "return a existing VatScheme response " in new Setup {
      val businessRegistrationSuccessResponse = Right(CurrentProfile("1", None, ""))

      when(mockBusRegConnector.retrieveCurrentProfile(any(), any())).thenReturn(businessRegistrationSuccessResponse)
      when(mockRegistrationRepository.retrieveVatScheme("1")).thenReturn(Some(vatScheme))

      val response = service.createNewRegistration()
      await(response.value) shouldBe Right(vatScheme)
    }

    "call to retrieveVatScheme return VatScheme from DB" in new Setup {
      when(mockRegistrationRepository.retrieveVatScheme("1")).thenReturn(Future.successful(Some(vatScheme)))
      val response = service.retrieveVatScheme("1")
      await(response.value) shouldBe Right(vatScheme)
    }

    "call to retrieveVatScheme return None from DB " in new Setup {
      when(mockRegistrationRepository.retrieveVatScheme("1")).thenReturn(Future.successful(None))
      val response = service.retrieveVatScheme("1")
      await(response.value) shouldBe Left(ResourceNotFound("1"))
    }

    "return a new VatScheme response " in new Setup {
      val businessRegistrationSuccessResponse = Right(CurrentProfile("1", None, ""))

      when(mockBusRegConnector.retrieveCurrentProfile(any(), any())).thenReturn(businessRegistrationSuccessResponse)
      when(mockRegistrationRepository.retrieveVatScheme(RegistrationId("1"))).thenReturn(None)
      when(mockRegistrationRepository.createNewVatScheme("1")).thenReturn(vatScheme)

      val response = service.createNewRegistration()
      await(response.value) shouldBe Right(vatScheme)
    }

    "error when creating VatScheme" in new Setup {
      val businessRegistrationSuccessResponse = Right(CurrentProfile("1", None, ""))
      val t = new Exception("Exception")

      when(mockBusRegConnector.retrieveCurrentProfile(any(), any())).thenReturn(businessRegistrationSuccessResponse)
      when(mockRegistrationRepository.retrieveVatScheme("1")).thenReturn(None)
      when(mockRegistrationRepository.createNewVatScheme("1")).thenReturn(Future.failed(t))

      val response = service.createNewRegistration()
      await(response.value) shouldBe Left(GenericError(t))
    }

    "error with the DB when creating VatScheme" in new Setup {
      val businessRegistrationSuccessResponse = Right(CurrentProfile("1", None, ""))
      val t = InsertFailed("regId", "VatScheme")

      when(mockBusRegConnector.retrieveCurrentProfile(any(), any())).thenReturn(businessRegistrationSuccessResponse)
      when(mockRegistrationRepository.retrieveVatScheme("1")).thenReturn(None)
      when(mockRegistrationRepository.createNewVatScheme("1")).thenReturn(Future.failed(t))

      val response = service.createNewRegistration()
      await(response.value) shouldBe Left(GenericDatabaseError(t, Some("regId")))
    }

    "call to business service return ForbiddenException response " in new Setup {
      when(mockBusRegConnector.retrieveCurrentProfile(any(), any())).thenReturn(Left(ForbiddenAccess("forbidden")))

      val response = service.createNewRegistration()
      await(response.value) shouldBe Left(ForbiddenAccess("forbidden"))
    }

    "call to business service return NotFoundException response " in new Setup {
      when(mockBusRegConnector.retrieveCurrentProfile(any(), any())).thenReturn(Left(ResourceNotFound("notfound")))

      val response = service.createNewRegistration()
      await(response.value) shouldBe Left(ResourceNotFound("notfound"))
    }

    "call to business service return ErrorResponse response " in new Setup {
      val t = new RuntimeException("Exception")
      when(mockBusRegConnector.retrieveCurrentProfile(any(), any())).thenReturn(Left(GenericError(t)))

      val response = service.createNewRegistration()
      await(response.value) shouldBe Left(GenericError(t))
    }

  }

  "call to updateLogicalGroup" should {

    val tradingDetails = VatTradingDetails("some-trader-name")

    "return Success response " in new Setup {
      when(mockRegistrationRepository.updateLogicalGroup("1", tradingDetails)).thenReturn(tradingDetails)
      val response = service.updateLogicalGroup("1", tradingDetails)
      await(response.value) shouldBe Right(tradingDetails)
    }

    "return Error response " in new Setup {
      val t = new RuntimeException("Exception")
      when(mockRegistrationRepository.updateLogicalGroup("1", tradingDetails)).thenReturn(Future.failed(t))

      val response = service.updateLogicalGroup("1", tradingDetails)
      await(response.value) shouldBe Left(GenericError(t))
    }

    "return Error response for MissingRegDocument" in new Setup {
      val regId = "regId"
      val t = MissingRegDocument(regId)
      when(mockRegistrationRepository.updateLogicalGroup(regId, tradingDetails)).thenReturn(Future.failed(t))

      val response = service.updateLogicalGroup(regId, tradingDetails)

      await(response.value) shouldBe Left(ResourceNotFound(s"No registration found for registration ID: $regId"))
    }
  }

  "call to deleteVatScheme" should {

    "return Success response " in new Setup {
      when(mockRegistrationRepository.deleteVatScheme("1")).thenReturn(Future.successful(true))
      val response = service.deleteVatScheme("1")
      await(response.value) shouldBe Right(true)
    }


    "return Error response " in new Setup {
      val t = new RuntimeException("Exception")
      when(mockRegistrationRepository.deleteVatScheme("1")).thenReturn(Future.failed(t))
      val response = service.deleteVatScheme("1")
      await(response.value) shouldBe Left(GenericError(t))
    }

    "return Error response for MissingRegDocument" in new Setup {
      val regId = "regId"
      val t = MissingRegDocument(regId)
      when(mockRegistrationRepository.deleteVatScheme("1")).thenReturn(Future.failed(t))
      val response = service.deleteVatScheme("1")
      await(response.value) shouldBe Left(ResourceNotFound(s"No registration found for registration ID: $regId"))
    }
  }

  "call to deleteBankAccountDetails" should {
    "return Success response " in new Setup {
      when(mockRegistrationRepository.deleteBankAccountDetails("1")).thenReturn(Future.successful(true))
      val response = service.deleteBankAccountDetails("1")
      await(response.value) shouldBe Right(true)
    }

    "return Error response " in new Setup {
      val t = new RuntimeException("Exception")
      when(mockRegistrationRepository.deleteBankAccountDetails("1")).thenReturn(Future.failed(t))
      val response = service.deleteBankAccountDetails("1")
      await(response.value) shouldBe Left(GenericError(t))
    }
  }

  "call to deleteAccountingPeriodStart" should {
    "return Success response " in new Setup {
      when(mockRegistrationRepository.deleteAccountingPeriodStart("1")).thenReturn(Future.successful(true))
      val response = service.deleteAccountingPeriodStart("1")
      await(response.value) shouldBe Right(true)
    }

    "return Error response " in new Setup {
      val t = new RuntimeException("Exception")
      when(mockRegistrationRepository.deleteAccountingPeriodStart("1")).thenReturn(Future.failed(t))
      val response = service.deleteAccountingPeriodStart("1")
      await(response.value) shouldBe Left(GenericError(t))
    }
  }

  "call to deleteZeroRatedTurnover" should {
    "return Success response " in new Setup {
      when(mockRegistrationRepository.deleteZeroRatedTurnover("1")).thenReturn(Future.successful(true))
      val response = service.deleteZeroRatedTurnover("1")
      await(response.value) shouldBe Right(true)
    }

    "return Error response " in new Setup {
      val t = new RuntimeException("Exception")
      when(mockRegistrationRepository.deleteZeroRatedTurnover("1")).thenReturn(Future.failed(t))
      val response = service.deleteZeroRatedTurnover("1")
      await(response.value) shouldBe Left(GenericError(t))
    }
  }

}

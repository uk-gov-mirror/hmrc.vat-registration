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

import common.Now
import common.exceptions._
import connectors._
import helpers.VatRegSpec
import models.external.CurrentProfile
import models._
import org.joda.time.DateTime
import org.mockito.Matchers
import org.mockito.Matchers.any
import org.mockito.Mockito._
import repositories.RegistrationRepository
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.Future

class VatRegistrationServiceSpec extends VatRegSpec {

  val mockBusRegConnector = mock[BusinessRegistrationConnector]
  val mockRegistrationRepository = mock[RegistrationRepository]
  val vatChoice: VatChoice = VatChoice.blank(new DateTime())


  trait Setup {
    val service = new VatRegistrationService(mockBusRegConnector, mockRegistrationRepository)
    implicit val defaultTimeOfNow: Now[DateTime] = Now(new DateTime(2017, 1, 31, 13, 6))
  }

  implicit val hc = HeaderCarrier()

  "createNewRegistration" should {
    "return a existing VatScheme response " in new Setup {
      val businessRegistrationSuccessResponse = Right(CurrentProfile("1", None, ""))
      val vatScheme = VatScheme.blank("1")

      when(mockBusRegConnector.retrieveCurrentProfile(any(), any())).thenReturn(businessRegistrationSuccessResponse)
      when(mockRegistrationRepository.retrieveVatScheme("1")).thenReturn(Some(vatScheme))

      val response = service.createNewRegistration
      await(response.value) shouldBe Right(vatScheme)
    }

    "return a new VatScheme response " in new Setup {
      val businessRegistrationSuccessResponse = Right(CurrentProfile("1", None, ""))
      val vatScheme = VatScheme.blank("1")

      when(mockBusRegConnector.retrieveCurrentProfile(any(), any())).thenReturn(businessRegistrationSuccessResponse)
      when(mockRegistrationRepository.retrieveVatScheme("1")).thenReturn(None)
      when(mockRegistrationRepository.createNewVatScheme(Matchers.eq("1"))(any())).thenReturn(vatScheme)

      val response = service.createNewRegistration
      await(response.value) shouldBe Right(vatScheme)
    }

    "error when creating VatScheme " in new Setup {
      val businessRegistrationSuccessResponse = Right(CurrentProfile("1", None, ""))
      val vatScheme = VatScheme.blank("1")
      val t = new Exception("Exception")

      when(mockBusRegConnector.retrieveCurrentProfile(any(), any())).thenReturn(businessRegistrationSuccessResponse)
      when(mockRegistrationRepository.retrieveVatScheme("1")).thenReturn(None)
      when(mockRegistrationRepository.createNewVatScheme(Matchers.eq("1"))(any())).thenReturn(Future.failed(t))

      val response = service.createNewRegistration
      await(response.value) shouldBe Left(GenericError(t))
    }

    "call to business service return ForbiddenException response " in new Setup {
      when(mockBusRegConnector.retrieveCurrentProfile(any(), any())).thenReturn(Left(Forbidden))

      val response = service.createNewRegistration
      await(response.value) shouldBe Left(Forbidden)
    }

    "call to business service return NotFoundException response " in new Setup {
      when(mockBusRegConnector.retrieveCurrentProfile(any(), any())).thenReturn(Left(NotFound))

      val response = service.createNewRegistration
      await(response.value) shouldBe Left(NotFound)
    }

    "call to business service return ErrorResponse response " in new Setup {
      val t = new RuntimeException("Exception")
      when(mockBusRegConnector.retrieveCurrentProfile(any(), any())).thenReturn(Left(GenericError(t)))

      val response = service.createNewRegistration
      await(response.value) shouldBe Left(GenericError(t))
    }

  }


  "call to updateVatChoice" should {

    "return Success response " in new Setup {
      when(mockRegistrationRepository.updateVatChoice("1", vatChoice)).thenReturn(vatChoice)

      val response = service.updateVatChoice("1", vatChoice)
      await(response.value) shouldBe Right(vatChoice)
    }


    "return Error response " in new Setup {
      val t = new RuntimeException("Exception")
      when(mockRegistrationRepository.updateVatChoice("1", vatChoice)).thenReturn(Future.failed(t))

      val response = service.updateVatChoice("1", vatChoice)
      await(response.value) shouldBe Left(GenericError(t))
    }

  }

  "call to updateTradingDetails" should {

    val tradingDetails = VatTradingDetails("some-trader-name")

    "return Success response " in new Setup {
      when(mockRegistrationRepository.updateTradingDetails("1", tradingDetails)).thenReturn(tradingDetails)

      val response = service.updateTradingDetails("1", tradingDetails)
      await(response.value) shouldBe Right(tradingDetails)
    }


    "return Error response " in new Setup {
      val t = new RuntimeException("Exception")
      when(mockRegistrationRepository.updateTradingDetails("1", tradingDetails)).thenReturn(Future.failed(t))

      val response = service.updateTradingDetails("1", tradingDetails)
      await(response.value) shouldBe Left(GenericError(t))
    }
  }

}

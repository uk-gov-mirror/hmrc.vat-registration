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
import common.exceptions.{ForbiddenException, GenericServiceException, NotFoundException}
import connectors._
import helpers.VatRegSpec
import models.VatScheme
import models.external.CurrentProfile
import org.joda.time.DateTime
import org.mockito.Matchers
import org.mockito.Mockito._
import repositories.RegistrationRepository
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.Future

class VatRegistrationServiceSpec extends VatRegSpec {

  val mockBusRegConnector = mock[BusinessRegistrationConnector]
  val mockRegistrationRepository = mock[RegistrationRepository]

  trait Setup {
    val service = new VatRegistrationService(mockBusRegConnector, mockRegistrationRepository)
    implicit val defaultTimeOfNow = Now(new DateTime(2017, 1, 31, 13, 6))
  }

  implicit val hc = HeaderCarrier()

  "createNewRegistration" should {
    "return a existing VatScheme response " in new Setup {
      val businessRegistrationSuccessResponse = BusinessRegistrationSuccessResponse(CurrentProfile("1", None, ""))
      val vatScheme = VatScheme.blank("1")

      when(mockBusRegConnector.retrieveCurrentProfile(Matchers.any(), Matchers.any())).thenReturn(businessRegistrationSuccessResponse)
      when(mockRegistrationRepository.retrieveRegistration("1")).thenReturn(Some(vatScheme))

      val response = service.createNewRegistration
      await(response) shouldBe Right(vatScheme)
    }

    "return a new VatScheme response " in new Setup {
      val businessRegistrationSuccessResponse = BusinessRegistrationSuccessResponse(CurrentProfile("1", None, ""))
      val vatScheme = VatScheme.blank("1")

      when(mockBusRegConnector.retrieveCurrentProfile(Matchers.any(), Matchers.any())).thenReturn(businessRegistrationSuccessResponse)
      when(mockRegistrationRepository.retrieveRegistration("1")).thenReturn(None)
      when(mockRegistrationRepository.createNewRegistration("1")).thenReturn(vatScheme)

      val response = service.createNewRegistration
      await(response) shouldBe Right(vatScheme)
    }

    "error when creating VatScheme " in new Setup {
      val businessRegistrationSuccessResponse = BusinessRegistrationSuccessResponse(CurrentProfile("1", None, ""))
      val vatScheme = VatScheme.blank("1")
      val t = new RuntimeException("Exception")

      when(mockBusRegConnector.retrieveCurrentProfile(Matchers.any(), Matchers.any())).thenReturn(businessRegistrationSuccessResponse)
      when(mockRegistrationRepository.retrieveRegistration("1")).thenReturn(None)
      when(mockRegistrationRepository.createNewRegistration("1")).thenReturn(Future.failed(t))


      val response = service.createNewRegistration
      await(response) shouldBe Left(GenericServiceException(t))
    }

    "call to business service return ForbiddenException response " in new Setup {
      when(mockBusRegConnector.retrieveCurrentProfile(Matchers.any(), Matchers.any())).thenReturn(BusinessRegistrationForbiddenResponse)

      val response = service.createNewRegistration
      await(response) shouldBe Left(ForbiddenException)
    }

    "call to business service return NotFoundException response " in new Setup {
      when(mockBusRegConnector.retrieveCurrentProfile(Matchers.any(), Matchers.any())).thenReturn(BusinessRegistrationNotFoundResponse)

      val response = service.createNewRegistration
      await(response) shouldBe Left(NotFoundException)
    }

    "call to business service return ErrorResponse response " in new Setup {
      val t = new RuntimeException("Exception")
      when(mockBusRegConnector.retrieveCurrentProfile(Matchers.any(), Matchers.any())).thenReturn(BusinessRegistrationErrorResponse(t))

      val response = service.createNewRegistration
      await(response) shouldBe Left(GenericServiceException(t))
    }

  }
}

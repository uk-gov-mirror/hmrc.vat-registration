/*
 * Copyright 2019 HM Revenue & Customs
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
import common.exceptions.MissingRegDocument
import fixtures.VatRegistrationFixture
import helpers.VatRegSpec
import models.api.TradingDetails
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import repositories.RegistrationMongoRepository
import scala.concurrent.ExecutionContext.Implicits.global

import scala.concurrent.Future

class TradingDetailsServiceSpec extends VatRegSpec with VatRegistrationFixture {

  class Setup {
    val service = new TradingDetailsService(
      registrationMongo = mockRegistrationMongo
    ) {
      override val registrationRepository: RegistrationMongoRepository = mockRegistrationMongoRepository
    }
  }

  "fetchTradingDetails" should {
    "return an Trading Details if found" in new Setup {
      when(mockRegistrationMongoRepository.retrieveTradingDetails(any())(any()))
        .thenReturn(Future.successful(Some(validFullTradingDetails)))

      val result: Option[TradingDetails] = await(service.retrieveTradingDetails("testId"))

      result shouldBe Some(validFullTradingDetails)
    }

    "return None if none found matching regId" in new Setup {
      when(mockRegistrationMongoRepository.retrieveTradingDetails(any())(any()))
        .thenReturn(Future.successful(None))

      val result: Option[TradingDetails] = await(service.retrieveTradingDetails("testId"))

      result shouldBe None
    }
  }

  "updateTradingDetails" should {
    "return the data that is being input" in new Setup {
      when(mockRegistrationMongoRepository.updateTradingDetails(any(),any())(any()))
        .thenReturn(Future.successful(validFullTradingDetails))

      val result: TradingDetails = await(service.updateTradingDetails("testId", validFullTradingDetails))

      result shouldBe validFullTradingDetails
    }

    "encounter an exception if an error occurs" in new Setup {
      when(mockRegistrationMongoRepository.updateTradingDetails(any(),any())(any()))
        .thenReturn(Future.failed(new Exception))

      intercept[Exception](await(service.updateTradingDetails("testId", validFullTradingDetails)))
    }

    "encounter a MissingRegDocument if no document is found" in new Setup {
      when(mockRegistrationMongoRepository.updateTradingDetails(any(), any())(any()))
        .thenReturn(Future.failed(MissingRegDocument(RegistrationId("testId"))))

      intercept[MissingRegDocument](await(service.updateTradingDetails("testId", validFullTradingDetails)))
    }
  }

}

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

import common.RegistrationId
import common.exceptions.MissingRegDocument
import fixtures.VatRegistrationFixture
import helpers.VatRegSpec
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import play.api.libs.json.JsObject
import play.api.test.FakeRequest
import repositories.RegistrationMongoRepository

import scala.concurrent.Future

class TradingDetailsControllerSpec extends VatRegSpec with VatRegistrationFixture {

  import play.api.test.Helpers._

  class Setup {
    val controller = new TradingDetailsControllerImpl(tradingDetailsService = mockTradingDetailsService, authConnector = mockAuthConnector){
      override val resourceConn: RegistrationMongoRepository = mockRegistrationMongoRepository
    }
  }

  "fetchTradingDetails" should {
    "return an Ok with valid trading details json if the document contains it" in new Setup {
      AuthorisationMocks.mockAuthorised(regId.value,internalid)
      when(mockTradingDetailsService.retrieveTradingDetails(any())(any()))
        .thenReturn(Future.successful(Some(validFullTradingDetails)))

      val result = controller.fetchTradingDetails("testId")(FakeRequest())
      status(result) shouldBe 200
      await(contentAsJson(result)) shouldBe validFullTradingDetailsJson
    }

    "return a NoContent if the trading details block is not present in the document" in new Setup {
      AuthorisationMocks.mockAuthorised(regId.value,internalid)
      when(mockTradingDetailsService.retrieveTradingDetails(any())(any()))
        .thenReturn(Future.successful(None))

      val result = controller.fetchTradingDetails("testId")(FakeRequest())
      status(result) shouldBe 204
    }

    "return NotFound if the registration document was not found for the regId provided" in new Setup {
      AuthorisationMocks.mockAuthorised(regId.value,internalid)
      when(mockTradingDetailsService.retrieveTradingDetails(any())(any()))
        .thenReturn(Future.failed(MissingRegDocument(RegistrationId("testId"))))

      val result = controller.fetchTradingDetails("testId")(FakeRequest())

      status(result) shouldBe 404
    }

    "return Forbidden if the registration document was not found for the regId provided" in new Setup {
      AuthorisationMocks.mockNotLoggedInOrAuthorised(regId.value)

      val result = controller.fetchTradingDetails("testId")(FakeRequest())
      status(result) shouldBe 403
    }
  }

  "updateTradingDetails" should {

    "returns Ok if successful" in new Setup {
      AuthorisationMocks.mockAuthorised(regId.value,internalid)
      when(mockTradingDetailsService.updateTradingDetails(any(), any())(any()))
        .thenReturn(Future.successful(validFullTradingDetails))

      val result = controller.updateTradingDetails("testId")(FakeRequest().withBody[JsObject](validFullTradingDetailsJson))

      status(result) shouldBe 200
      await(contentAsJson(result)) shouldBe validFullTradingDetailsJson
    }

    "returns NotFound if the registration is not found" in new Setup {
      AuthorisationMocks.mockAuthorised(regId.value,internalid)
      when(mockTradingDetailsService.updateTradingDetails(any(), any())(any()))
        .thenReturn(Future.failed(MissingRegDocument(RegistrationId("testId"))))

      val result = controller.updateTradingDetails("testId")(FakeRequest().withBody[JsObject](validFullTradingDetailsJson))
      status(result) shouldBe 404
    }

    "returns InternalServerError if an error occurs" in new Setup {
      AuthorisationMocks.mockAuthorised(regId.value,internalid)
      when(mockTradingDetailsService.updateTradingDetails(any(), any())(any()))
        .thenReturn(Future.failed(new Exception))

      val result = controller.updateTradingDetails("testId")(FakeRequest().withBody[JsObject](validFullTradingDetailsJson))
      status(result) shouldBe 500
    }

    "returns Forbidden if user is not authorised" in new Setup {
      AuthorisationMocks.mockNotLoggedInOrAuthorised(regId.value)

      val result = controller.updateTradingDetails("testId")(FakeRequest().withBody[JsObject](validFullTradingDetailsJson))
      status(result) shouldBe 403
    }

  }
}
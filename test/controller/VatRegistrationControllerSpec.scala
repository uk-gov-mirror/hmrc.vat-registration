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

package controller

import java.time.LocalDate

import akka.stream.Materializer
import controllers.VatRegistrationController
import helpers.VatRegSpec
import models.{VatAccountingPeriod, _}
import play.api.libs.json.Json
import play.api.mvc.Result
import play.api.mvc.Results.{Created, Accepted}
import play.api.test.FakeRequest
import play.api.test.Helpers._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


class VatRegistrationControllerSpec extends VatRegSpec {

  val testId = "testId"
  val date = LocalDate.of(2017, 1, 1)
  val vatChoice: VatChoice = VatChoice(date, "obligatory")
  val tradingDetails: VatTradingDetails = VatTradingDetails("some-trader-name")
  val vatScheme: VatScheme = VatScheme(testId, None, None, None)
  val materializer = fakeApplication.injector.instanceOf[Materializer]

  class Setup {
    val controller = new VatRegistrationController(mockAuthConnector, mockRegistrationService)
  }

  "GET /" should {

    "return 403 if user not authenticated" in new Setup {
      AuthorisationMocks.mockNotLoggedInOrAuthorised
      val response: Future[Result] = controller.newVatRegistration(FakeRequest())
      status(response) shouldBe FORBIDDEN
    }

    "return 201 if a new VAT scheme is successfully created" in new Setup {
      AuthorisationMocks.mockSuccessfulAuthorisation(testAuthority(testId))
      ServiceMocks.mockSuccessfulCreateNewRegistration(testId)
      val response: Future[Result] = controller.newVatRegistration()(FakeRequest())
      status(response) shouldBe CREATED
    }


    "return 503 if RegistrationService encounters any problems" in new Setup {
      AuthorisationMocks.mockSuccessfulAuthorisation(testAuthority(testId))
      ServiceMocks.mockFailedCreateNewRegistration(testId)
      val response: Future[Result] = controller.newVatRegistration()(FakeRequest())
      status(response) shouldBe SERVICE_UNAVAILABLE
    }

    "return 503 if RegistrationService encounters any problems with the DB" in new Setup {
      AuthorisationMocks.mockSuccessfulAuthorisation(testAuthority(testId))
      ServiceMocks.mockFailedCreateNewRegistrationWithDbError(testId)
      val response: Future[Result] = controller.newVatRegistration()(FakeRequest())
      status(response) shouldBe SERVICE_UNAVAILABLE
    }



    "call updateVatChoice return ACCEPTED" in new Setup {
      AuthorisationMocks.mockSuccessfulAuthorisation(testAuthority(testId))
      ServiceMocks.mockSuccessfulUpdateVatChoice(testId, vatChoice)
      val response: Future[Result] = controller.updateVatChoice(testId)(
        FakeRequest().withBody(Json.toJson[VatChoice](vatChoice)))
      await(response) shouldBe Accepted(Json.toJson(vatChoice))
    }

    "call updateVatChoice return ServiceUnavailable" in new Setup {
      AuthorisationMocks.mockSuccessfulAuthorisation(testAuthority(testId))
      val exception = new Exception("Exception")
      ServiceMocks.mockServiceUnavailableUpdateVatChoice(testId, vatChoice, exception)
      val response: Future[Result] = controller.updateVatChoice(testId)(
        FakeRequest().withBody(Json.toJson[VatChoice](vatChoice)))
      status(response) shouldBe SERVICE_UNAVAILABLE
    }

    "updateTradingDetails" should {

      val fakeRequest = FakeRequest().withBody(Json.toJson(tradingDetails))

      "call updateTradingDetails return ACCEPTED" in new Setup {
        AuthorisationMocks.mockSuccessfulAuthorisation(testAuthority(testId))
        ServiceMocks.mockSuccessfulUpdateTradingDetails(testId, tradingDetails)
        val response: Future[Result] = controller.updateTradingDetails(testId)(fakeRequest)
        await(response) shouldBe Accepted(Json.toJson(tradingDetails))
      }

      "call updateTradingDetails return ServiceUnavailable" in new Setup {
        AuthorisationMocks.mockSuccessfulAuthorisation(testAuthority(testId))
        val exception = new Exception("Exception")
        ServiceMocks.mockServiceUnavailableUpdateTradingDetails(testId, tradingDetails, exception)
        val response: Future[Result] = controller.updateTradingDetails(testId)(fakeRequest)
        status(response) shouldBe SERVICE_UNAVAILABLE
      }

      "call to retrieveVatScheme return Ok with VatScheme" in new Setup {
        AuthorisationMocks.mockSuccessfulAuthorisation(testAuthority(testId))
        ServiceMocks.mockRetrieveVatScheme(testId, vatScheme)
        val response: Future[Result] = controller.retrieveVatScheme(testId)(
          FakeRequest()
        )
        status(response) shouldBe OK
        response.map(_ shouldBe vatScheme)
      }

      "call to retrieveVatScheme return ServiceUnavailable" in new Setup {
        AuthorisationMocks.mockSuccessfulAuthorisation(testAuthority(testId))
        ServiceMocks.mockRetrieveVatSchemeThrowsException(testId)
        val response: Future[Result] = controller.retrieveVatScheme(testId)(FakeRequest())
        status(response) shouldBe SERVICE_UNAVAILABLE
      }

      "call to deleteVatScheme return Ok with VatScheme" in new Setup {
        AuthorisationMocks.mockSuccessfulAuthorisation(testAuthority(testId))
        ServiceMocks.mockDeleteVatScheme(testId)
        val response: Future[Result] = controller.deleteVatScheme(testId)(
          FakeRequest()
        )
        status(response) shouldBe OK
        response.map(_ shouldBe true)
      }

      "call to deleteVatScheme return ServiceUnavailable" in new Setup {
        AuthorisationMocks.mockSuccessfulAuthorisation(testAuthority(testId))
        ServiceMocks.mockDeleteVatSchemeThrowsException(testId)
        val response: Future[Result] = controller.deleteVatScheme(testId)(FakeRequest())
        status(response) shouldBe SERVICE_UNAVAILABLE
      }

    }

    "updateVatFinancials" should {

      val EstimateValue: Long = 10000000000L
      val zeroRatedTurnoverEstimate : Long = 10000000000L
      val vatFinancials = VatFinancials(Some(VatBankAccount("Reddy", "10-01-01","12345678")),
        EstimateValue,
        Some(zeroRatedTurnoverEstimate),
        true,
        VatAccountingPeriod(None, "monthly")
      )

      val fakeRequest = FakeRequest().withBody(Json.toJson(vatFinancials))

      "call updateVatFinancials return ACCEPTED" in new Setup {
        AuthorisationMocks.mockSuccessfulAuthorisation(testAuthority(testId))
        ServiceMocks.mockSuccessfulUpdateVatFinancials(testId, vatFinancials)
        val response: Future[Result] = controller.updateVatFinancials(testId)(fakeRequest)
        await(response) shouldBe Accepted(Json.toJson(vatFinancials))
      }

      "call updateVatFinancials return ServiceUnavailable" in new Setup {
        AuthorisationMocks.mockSuccessfulAuthorisation(testAuthority(testId))
        val exception = new Exception("Exception")
        ServiceMocks.mockServiceUnavailableUpdateVatFinancials(testId, vatFinancials, exception)
        val response: Future[Result] = controller.updateVatFinancials(testId)(fakeRequest)
        status(response) shouldBe SERVICE_UNAVAILABLE
      }

    }

  }

}
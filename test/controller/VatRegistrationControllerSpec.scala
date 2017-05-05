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

import common.RegistrationId
import controllers.VatRegistrationController
import helpers.VatRegSpec
import models._
import models.api._
import play.api.libs.json.Json
import play.api.mvc.Result
import play.api.mvc.Results.Accepted
import play.api.test.FakeRequest
import play.api.test.Helpers._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


class VatRegistrationControllerSpec extends VatRegSpec {

  val regId = RegistrationId("testId")
  val userId = "userId"
  val date = LocalDate.of(2017, 1, 1)
  val vatChoice: VatChoice = VatChoice(
    necessity = "obligatory",
    vatStartDate = VatStartDate(
      selection = "SPECIFIC_DATE",
      startDate = Some(date)))
  val tradingDetails: VatTradingDetails = VatTradingDetails(
    vatChoice = vatChoice,
    tradingName = TradingName(
      selection = true,
      tradingName = Some("some-trader-name")),
    euTrading = VatEuTrading(true, Some(true))
  )
  val sicAndCompliance: VatSicAndCompliance = VatSicAndCompliance("some-business-description", None, None)
  val vatDigitalContact = VatDigitalContact("test@test.com", Some("12345678910"), Some("12345678910"))
  val vatContact = VatContact(vatDigitalContact)
  val vatEligibility = VatServiceEligibility(
    haveNino = Some(true),
    doingBusinessAbroad = Some(true),
    doAnyApplyToYou = Some(true),
    applyingForAnyOf = Some(true),
    companyWillDoAnyOf = Some(true)
  )
  val scrsAddress = ScrsAddress("line1", "line2", None, None, Some("XX XX"), Some("UK"))
  val vatLodgingOfficer = VatLodgingOfficer(scrsAddress)

  val vatScheme: VatScheme = VatScheme(regId)

  class Setup {
    val controller = new VatRegistrationController(mockAuthConnector, mockRegistrationService)
  }

  "GET /" should {

    "return 403 if user not authenticated" in new Setup {
      AuthorisationMocks.mockNotLoggedInOrAuthorised()
      val response: Future[Result] = controller.newVatRegistration(FakeRequest())
      status(response) shouldBe FORBIDDEN
    }

    "return 201 if a new VAT scheme is successfully created" in new Setup {
      AuthorisationMocks.mockSuccessfulAuthorisation(testAuthority(userId))
      ServiceMocks.mockSuccessfulCreateNewRegistration(regId)
      val response: Future[Result] = controller.newVatRegistration()(FakeRequest())
      status(response) shouldBe CREATED
    }

    "call to retrieveVatScheme return Ok with VatScheme" in new Setup {
      AuthorisationMocks.mockSuccessfulAuthorisation(testAuthority(userId))
      ServiceMocks.mockRetrieveVatScheme(regId, vatScheme)
      val response: Future[Result] = controller.retrieveVatScheme(regId)(
        FakeRequest()
      )
      status(response) shouldBe OK
      response.map(_ shouldBe vatScheme)
    }

    "call to retrieveVatScheme return ServiceUnavailable" in new Setup {
      AuthorisationMocks.mockSuccessfulAuthorisation(testAuthority(userId))
      ServiceMocks.mockRetrieveVatSchemeThrowsException(regId)
      val response: Future[Result] = controller.retrieveVatScheme(regId)(FakeRequest())
      status(response) shouldBe SERVICE_UNAVAILABLE
    }

    "return 503 if RegistrationService encounters any problems" in new Setup {
      AuthorisationMocks.mockSuccessfulAuthorisation(testAuthority(userId))
      ServiceMocks.mockFailedCreateNewRegistration(regId)
      val response: Future[Result] = controller.newVatRegistration()(FakeRequest())
      status(response) shouldBe SERVICE_UNAVAILABLE
    }

    "return 503 if RegistrationService encounters any problems with the DB" in new Setup {
      AuthorisationMocks.mockSuccessfulAuthorisation(testAuthority(userId))
      ServiceMocks.mockFailedCreateNewRegistrationWithDbError(regId)
      val response: Future[Result] = controller.newVatRegistration()(FakeRequest())
      status(response) shouldBe SERVICE_UNAVAILABLE
    }

    "updateVatFinancials" should {

      val vatFinancials = VatFinancials(Some(VatBankAccount("Reddy", "10-01-01", "12345678")),
        turnoverEstimate = 10000000000L,
        zeroRatedTurnoverEstimate = Some(10000000000L),
        reclaimVatOnMostReturns = true,
        accountingPeriods = VatAccountingPeriod("monthly")
      )

      val fakeRequest = FakeRequest().withBody(Json.toJson(vatFinancials))

      "call updateVatFinancials return ACCEPTED" in new Setup {
        AuthorisationMocks.mockSuccessfulAuthorisation(testAuthority(userId))
        ServiceMocks.mockSuccessfulUpdateLogicalGroup(vatFinancials)
        val response: Future[Result] = controller.updateVatFinancials(regId)(fakeRequest)
        val expectedJson = Json.toJson(vatFinancials)
        status(response) shouldBe ACCEPTED
      }

      "call updateVatFinancials return ServiceUnavailable" in new Setup {
        AuthorisationMocks.mockSuccessfulAuthorisation(testAuthority(userId))
        val exception = new Exception("Exception")
        ServiceMocks.mockServiceUnavailableUpdateLogicalGroup(vatFinancials, exception)
        val response: Future[Result] = controller.updateVatFinancials(regId)(fakeRequest)
        status(response) shouldBe SERVICE_UNAVAILABLE
      }

    }

    "updateTradingDetails" should {

      val fakeRequest = FakeRequest().withBody(Json.toJson(tradingDetails))

      "call updateTradingDetails return ACCEPTED" in new Setup {
        AuthorisationMocks.mockSuccessfulAuthorisation(testAuthority(userId))
        ServiceMocks.mockSuccessfulUpdateLogicalGroup(tradingDetails)
        val response: Future[Result] = controller.updateTradingDetails(regId)(fakeRequest)
        await(response) shouldBe Accepted(Json.toJson(tradingDetails))
      }

      "call updateTradingDetails return ServiceUnavailable" in new Setup {
        AuthorisationMocks.mockSuccessfulAuthorisation(testAuthority(userId))
        val exception = new Exception("Exception")
        ServiceMocks.mockServiceUnavailableUpdateLogicalGroup(tradingDetails, exception)
        val response: Future[Result] = controller.updateTradingDetails(regId)(fakeRequest)
        status(response) shouldBe SERVICE_UNAVAILABLE
      }

    }

    "updateSicAndCompliance" should {

      val fakeRequest = FakeRequest().withBody(Json.toJson(sicAndCompliance))

      "call updateSicAndCompliance return ACCEPTED" in new Setup {
        AuthorisationMocks.mockSuccessfulAuthorisation(testAuthority(userId))
        ServiceMocks.mockSuccessfulUpdateLogicalGroup(sicAndCompliance)
        val response: Future[Result] = controller.updateSicAndCompliance(regId)(fakeRequest)
        await(response) shouldBe Accepted(Json.toJson(sicAndCompliance))
      }

      "call updateSicAndCompliance return ServiceUnavailable" in new Setup {
        AuthorisationMocks.mockSuccessfulAuthorisation(testAuthority(userId))
        val exception = new Exception("Exception")
        ServiceMocks.mockServiceUnavailableUpdateLogicalGroup(sicAndCompliance, exception)
        val response: Future[Result] = controller.updateSicAndCompliance(regId)(fakeRequest)
        status(response) shouldBe SERVICE_UNAVAILABLE
      }

    }

    "updateVatContact" should {

      val fakeRequest = FakeRequest().withBody(Json.toJson(vatContact))

      "call updateVatContact return ACCEPTED" in new Setup {
        AuthorisationMocks.mockSuccessfulAuthorisation(testAuthority(userId))
        ServiceMocks.mockSuccessfulUpdateLogicalGroup(vatContact)
        val response: Future[Result] = controller.updateVatContact(regId)(fakeRequest)
        await(response) shouldBe Accepted(Json.toJson(vatContact))
      }

      "call updateVatContact return ServiceUnavailable" in new Setup {
        AuthorisationMocks.mockSuccessfulAuthorisation(testAuthority(userId))
        val exception = new Exception("Exception")
        ServiceMocks.mockServiceUnavailableUpdateLogicalGroup(vatContact, exception)
        val response: Future[Result] = controller.updateVatContact(regId)(fakeRequest)
        status(response) shouldBe SERVICE_UNAVAILABLE
      }

    }

    "updateVatEligibility" should {

      val fakeRequest = FakeRequest().withBody(Json.toJson(vatEligibility))

      "call updateVatEligibility return ACCEPTED" in new Setup {
        AuthorisationMocks.mockSuccessfulAuthorisation(testAuthority(userId))
        ServiceMocks.mockSuccessfulUpdateLogicalGroup(vatEligibility)
        val response: Future[Result] = controller.updateVatEligibility(regId)(fakeRequest)
        await(response) shouldBe Accepted(Json.toJson(vatEligibility))
      }

      "call updateVatEligibility return ServiceUnavailable" in new Setup {
        AuthorisationMocks.mockSuccessfulAuthorisation(testAuthority(userId))
        val exception = new Exception("Exception")
        ServiceMocks.mockServiceUnavailableUpdateLogicalGroup(vatEligibility, exception)
        val response: Future[Result] = controller.updateVatEligibility(regId)(fakeRequest)
        status(response) shouldBe SERVICE_UNAVAILABLE
      }
    }

      "updateVatLodgingOfficer" should {

        val fakeRequest = FakeRequest().withBody(Json.toJson(vatLodgingOfficer))

        "call updateVatLodgingOfficer return ACCEPTED" in new Setup {
          AuthorisationMocks.mockSuccessfulAuthorisation(testAuthority(userId))
          ServiceMocks.mockSuccessfulUpdateLogicalGroup(vatLodgingOfficer)
          val response: Future[Result] = controller.updateLodgingOfficer(regId)(fakeRequest)
          await(response) shouldBe Accepted(Json.toJson(vatLodgingOfficer))
        }

        "call updateVatLodgingOfficer return ServiceUnavailable" in new Setup {
          AuthorisationMocks.mockSuccessfulAuthorisation(testAuthority(userId))
          val exception = new Exception("Exception")
          ServiceMocks.mockServiceUnavailableUpdateLogicalGroup(vatLodgingOfficer, exception)
          val response: Future[Result] = controller.updateLodgingOfficer(regId)(fakeRequest)
          status(response) shouldBe SERVICE_UNAVAILABLE
        }
      }
    "deleteVatScheme" should {
      "call to deleteVatScheme return Ok with VatScheme" in new Setup {
        AuthorisationMocks.mockSuccessfulAuthorisation(testAuthority(userId))
        ServiceMocks.mockDeleteVatScheme(regId)
        val response: Future[Result] = controller.deleteVatScheme(regId)(FakeRequest())
        status(response) shouldBe OK
      }

      "call to deleteVatScheme return ServiceUnavailable" in new Setup {
        AuthorisationMocks.mockSuccessfulAuthorisation(testAuthority(userId))
        ServiceMocks.mockDeleteVatSchemeThrowsException(regId)
        val response: Future[Result] = controller.deleteVatScheme(regId)(FakeRequest())
        status(response) shouldBe SERVICE_UNAVAILABLE
      }
    }

    "deleteByElement" should {
      "call to deleteByElement return Ok with VatScheme" in new Setup {
        AuthorisationMocks.mockSuccessfulAuthorisation(testAuthority(userId))
        ServiceMocks.mockDeleteByElement(regId, VatBankAccountPath)
        val response: Future[Result] = controller.deleteByElement(regId, VatBankAccountPath)(FakeRequest())
        status(response) shouldBe OK
      }

      "call to deleteBankAccountDetails return ServiceUnavailable" in new Setup {
        AuthorisationMocks.mockSuccessfulAuthorisation(testAuthority(userId))
        ServiceMocks.mockDeleteByElementThrowsException(regId, VatBankAccountPath)
        val response: Future[Result] = controller.deleteByElement(regId, VatBankAccountPath)(FakeRequest())
        status(response) shouldBe SERVICE_UNAVAILABLE
      }
    }

  }

}
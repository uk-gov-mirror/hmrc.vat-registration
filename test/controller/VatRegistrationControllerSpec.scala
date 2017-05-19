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
import play.api.mvc.Results.Accepted
import play.api.test.FakeRequest
import play.api.test.Helpers._

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
    euTrading = VatEuTrading(selection = true, eoriApplication = Some(true))
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
  val name = Name(forename = Some("Forename"), surname = Some("Surname"), title = Some("Title"))
  val vatLodgingOfficer = VatLodgingOfficer(scrsAddress, DateOfBirth(1, 1, 1980), "NB666666C", "director", name)

  val vatScheme: VatScheme = VatScheme(regId)
  val exception = new Exception("Exception")

  class Setup {
    val controller = new VatRegistrationController(mockAuthConnector, mockRegistrationService)
    AuthorisationMocks.mockSuccessfulAuthorisation(testAuthority(userId))
  }

  "GET /" should {

    "return 403 if user not authenticated" in new Setup {
      AuthorisationMocks.mockNotLoggedInOrAuthorised()
      controller.newVatRegistration(FakeRequest()) returnsStatus FORBIDDEN
    }

    "return 201 if a new VAT scheme is successfully created" in new Setup {
      ServiceMocks.mockSuccessfulCreateNewRegistration(regId)
      controller.newVatRegistration()(FakeRequest()) returnsStatus CREATED
    }

    "call to retrieveVatScheme return Ok with VatScheme" in new Setup {
      ServiceMocks.mockRetrieveVatScheme(regId, vatScheme)
      controller.retrieveVatScheme(regId)(FakeRequest()) returnsStatus OK
    }

    "call to retrieveVatScheme return ServiceUnavailable" in new Setup {
      ServiceMocks.mockRetrieveVatSchemeThrowsException(regId)
      controller.retrieveVatScheme(regId)(FakeRequest()) returnsStatus SERVICE_UNAVAILABLE
    }

    "return 503 if RegistrationService encounters any problems" in new Setup {
      ServiceMocks.mockFailedCreateNewRegistration(regId)
      controller.newVatRegistration()(FakeRequest()) returnsStatus SERVICE_UNAVAILABLE
    }

    "return 503 if RegistrationService encounters any problems with the DB" in new Setup {
      ServiceMocks.mockFailedCreateNewRegistrationWithDbError(regId)
      controller.newVatRegistration()(FakeRequest()) returnsStatus SERVICE_UNAVAILABLE
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
        ServiceMocks.mockSuccessfulUpdateLogicalGroup(vatFinancials)
        controller.updateVatFinancials(regId)(fakeRequest) returnsStatus ACCEPTED
      }

      "call updateVatFinancials return ServiceUnavailable" in new Setup {
        ServiceMocks.mockServiceUnavailableUpdateLogicalGroup(vatFinancials, exception)
        controller.updateVatFinancials(regId)(fakeRequest) returnsStatus SERVICE_UNAVAILABLE
      }

    }

    "updateTradingDetails" should {

      val fakeRequest = FakeRequest().withBody(Json.toJson(tradingDetails))

      "call updateTradingDetails return ACCEPTED" in new Setup {
        ServiceMocks.mockSuccessfulUpdateLogicalGroup(tradingDetails)
        controller.updateTradingDetails(regId)(fakeRequest) returns Accepted(Json.toJson(tradingDetails))
      }

      "call updateTradingDetails return ServiceUnavailable" in new Setup {
        ServiceMocks.mockServiceUnavailableUpdateLogicalGroup(tradingDetails, exception)
        controller.updateTradingDetails(regId)(fakeRequest) returnsStatus SERVICE_UNAVAILABLE
      }

    }

    "updateSicAndCompliance" should {

      val fakeRequest = FakeRequest().withBody(Json.toJson(sicAndCompliance))

      "call updateSicAndCompliance return ACCEPTED" in new Setup {
        ServiceMocks.mockSuccessfulUpdateLogicalGroup(sicAndCompliance)
        controller.updateSicAndCompliance(regId)(fakeRequest) returns Accepted(Json.toJson(sicAndCompliance))
      }

      "call updateSicAndCompliance return ServiceUnavailable" in new Setup {
        ServiceMocks.mockServiceUnavailableUpdateLogicalGroup(sicAndCompliance, exception)
        controller.updateSicAndCompliance(regId)(fakeRequest) returnsStatus SERVICE_UNAVAILABLE
      }

    }

    "updateVatContact" should {

      val fakeRequest = FakeRequest().withBody(Json.toJson(vatContact))

      "call updateVatContact return ACCEPTED" in new Setup {
        ServiceMocks.mockSuccessfulUpdateLogicalGroup(vatContact)
        controller.updateVatContact(regId)(fakeRequest) returns Accepted(Json.toJson(vatContact))
      }

      "call updateVatContact return ServiceUnavailable" in new Setup {
        ServiceMocks.mockServiceUnavailableUpdateLogicalGroup(vatContact, exception)
        controller.updateVatContact(regId)(fakeRequest) returnsStatus SERVICE_UNAVAILABLE
      }

    }

    "updateVatEligibility" should {

      val fakeRequest = FakeRequest().withBody(Json.toJson(vatEligibility))

      "call updateVatEligibility return ACCEPTED" in new Setup {
        ServiceMocks.mockSuccessfulUpdateLogicalGroup(vatEligibility)
        controller.updateVatEligibility(regId)(fakeRequest) returns Accepted(Json.toJson(vatEligibility))
      }

      "call updateVatEligibility return ServiceUnavailable" in new Setup {
        ServiceMocks.mockServiceUnavailableUpdateLogicalGroup(vatEligibility, exception)
        controller.updateVatEligibility(regId)(fakeRequest) returnsStatus SERVICE_UNAVAILABLE
      }
    }

    "updateVatLodgingOfficer" should {

      val fakeRequest = FakeRequest().withBody(Json.toJson(vatLodgingOfficer))

      "call updateVatLodgingOfficer return ACCEPTED" in new Setup {
        ServiceMocks.mockSuccessfulUpdateLogicalGroup(vatLodgingOfficer)
        controller.updateLodgingOfficer(regId)(fakeRequest) returns Accepted(Json.toJson(vatLodgingOfficer))
      }

      "call updateVatLodgingOfficer return ServiceUnavailable" in new Setup {
        ServiceMocks.mockServiceUnavailableUpdateLogicalGroup(vatLodgingOfficer, exception)
        controller.updateLodgingOfficer(regId)(fakeRequest) returnsStatus SERVICE_UNAVAILABLE
      }
    }

    "deleteVatScheme" should {
      "call to deleteVatScheme return Ok with VatScheme" in new Setup {
        ServiceMocks.mockDeleteVatScheme(regId)
        controller.deleteVatScheme(regId)(FakeRequest()) returnsStatus OK
      }

      "call to deleteVatScheme return ServiceUnavailable" in new Setup {
        ServiceMocks.mockDeleteVatSchemeThrowsException(regId)
        controller.deleteVatScheme(regId)(FakeRequest()) returnsStatus SERVICE_UNAVAILABLE
      }
    }

    "deleteByElement" should {
      "call to deleteByElement return Ok with VatScheme" in new Setup {
        ServiceMocks.mockDeleteByElement(regId, VatBankAccountPath)
        controller.deleteByElement(regId, VatBankAccountPath)(FakeRequest()) returnsStatus OK
      }

      "call to deleteBankAccountDetails return ServiceUnavailable" in new Setup {
        ServiceMocks.mockDeleteByElementThrowsException(regId, VatBankAccountPath)
        controller.deleteByElement(regId, VatBankAccountPath)(FakeRequest()) returnsStatus SERVICE_UNAVAILABLE
      }
    }

  }

}

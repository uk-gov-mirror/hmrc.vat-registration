/*
 * Copyright 2021 HM Revenue & Customs
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

package models.submission

import fixtures.{VatRegistrationFixture, VatSubmissionFixture}
import helpers.BaseSpec
import models.JsonFormatValidation
import models.api.{BankAccount, SicAndCompliance, SicCode}
import play.api.libs.json._

class VatSubmissionSpec extends BaseSpec with JsonFormatValidation with VatRegistrationFixture with VatSubmissionFixture {

  val testMessageType = "SubmissionCreate"
  val testSafeID = "12345678901234567890"
  val testLine1 = "line1"
  val testLine2 = "line2"
  val testPostCode = "A11 11A"
  val testCountry = "GB"

  val testVatSubmission: VatSubmission = VatSubmission(
    testMessageType,
    Some(UkCompany),
    Some(true),
    Some(testCrn),
    validApplicantDetails,
    Some(testBankAccount),
    testSicAndCompliance.get,
    testBusinessContact.get,
    validFullTradingDetails,
    Some(validFullFRSDetails),
    testEligibilitySubmissionData,
    testReturns
  )

  val testSicResult = SicAndCompliance(
    businessDescription = "this is my business description",
    labourCompliance = testSicAndCompliance.flatMap(_.labourCompliance),
    mainBusinessActivity = SicCode("12345", "", ""),
    businessActivities = List(SicCode("00998", "", ""), SicCode("00889", "", ""))
  )

  "converting a Json from the Mongo DB" should {
    "produce a valid VatSubmission model" in {
      val model = Json.fromJson[VatSubmission](mongoJson)

      model mustBe JsSuccess(testVatSubmission)
    }
  }

  "converting from the VatScheme stored in Mongo" should {
    "return a VatSubmission model when all required data is present" in {
      val scheme = testVatScheme.copy(
        applicantDetails = Some(validApplicantDetails),
        businessContact = testBusinessContact,
        bankAccount = Some(BankAccount(true, Some(testBankDetails), None)),
        sicAndCompliance = testSicAndCompliance,
        flatRateScheme = Some(validFullFlatRateScheme),
        tradingDetails = Some(validFullTradingDetails),
        eligibilitySubmissionData = Some(testEligibilitySubmissionData),
        returns = Some(testReturns.copy(zeroRatedSupplies = Some(zeroRatedSupplies))),
        confirmInformationDeclaration = Some(true)
      )

      val res = VatSubmission.fromVatScheme(scheme)

      res mustBe VatSubmission(
        tradersPartyType = Some(UkCompany),
        confirmInformationDeclaration = Some(true),
        companyRegistrationNumber = Some("CRN"),
        applicantDetails = validApplicantDetails,
        bankDetails = Some(testBankAccount),
        sicAndCompliance = testSicAndCompliance.get,
        businessContact = testBusinessContact.get,
        tradingDetails = validFullTradingDetails,
        flatRateScheme = validFullFlatRateScheme.frsDetails,
        eligibilitySubmissionData = testEligibilitySubmissionData,
        returns = testReturns
      )
    }

    "throw an IllegalStateException when required data is missing" in {
      intercept[IllegalStateException] {
        VatSubmission.fromVatScheme(testVatScheme)
      }
    }
  }
}

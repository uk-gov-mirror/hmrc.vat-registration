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

package models

import fixtures.{VatRegistrationFixture, VatSubmissionFixture}
import helpers.BaseSpec
import models.api.{BankAccount, MTDfB, SicAndCompliance, SicCode, VatSubmission}
import play.api.libs.json.{JsSuccess, Json}

class VatSubmissionSpec extends BaseSpec with JsonFormatValidation with VatRegistrationFixture with VatSubmissionFixture {

  val testMessageType = "SubmissionCreate"
  val testTradersPartyType = "50"
  val testSafeID = "12345678901234567890"
  val testLine1 = "line1"
  val testLine2 = "line2"
  val testPostCode = "A11 11A"
  val testCountry = "GB"

  val testVatSubmission: VatSubmission = VatSubmission(
    testMessageType,
    Some(testTradersPartyType),
    Some(testSafeID),
    Some(true),
    Some(testCrn),
    validApplicantDetails,
    Some(testBankDetails),
    testSicAndCompliance.get,
    testBusinessContact.get,
    validFullTradingDetails,
    Some(validFullFRSDetails),
    testEligibilitySubmissionData
  )

  "converting a VatSubmission model into Json" should {
    "produce a valid Json for a DES submission" in {
      val json = Json.toJson(testVatSubmission)(VatSubmission.submissionFormat)

      json mustBe vatSubmissionJson
    }

    "produce a Json to store it in a Mongo DB" in {
      val json = Json.toJson(testVatSubmission)

      json mustBe mongoJson
    }
  }

  "converting a Json from the Mongo DB" should {
    "produce a valid VatSubmission model" in {
      val model = Json.fromJson[VatSubmission](mongoJson)

      model mustBe JsSuccess(testVatSubmission)
    }
  }

  "validating Json" should {
    "successfully validate a valid json submission" in {
      val expectedSic = SicAndCompliance(
        businessDescription = "this is my business description",
        labourCompliance = None,
        mainBusinessActivity = SicCode("12345", "", ""),
        otherBusinessActivities = List(SicCode("00998", "", ""), SicCode("00889", "", ""))
      )

      val expectedFrs = validFullFRSDetails.copy(businessGoods = None)

      val expected = testVatSubmission.copy(
        sicAndCompliance = expectedSic,
        flatRateScheme = Some(expectedFrs)
      )

      val result = Json.fromJson[VatSubmission](vatSubmissionJson)(VatSubmission.submissionFormat)

      result mustBe JsSuccess(expected)
    }
  }

  "converting from the VatScheme stored in Mongo" should {
    "return a VatSubmission model when all required data is present" in {
      val scheme = testVatScheme.copy(
        applicantDetails = Some(validApplicantDetails),
        businessContact = testBusinessContact,
        bankAccount = Some(BankAccount(true, Some(testBankDetails))),
        sicAndCompliance = testSicAndCompliance,
        flatRateScheme = Some(validFullFlatRateScheme),
        tradingDetails = Some(validFullTradingDetails),
        eligibilitySubmissionData = Some(testEligibilitySubmissionData)
      )

      val res = VatSubmission.fromVatScheme(scheme)

      res mustBe VatSubmission(
        tradersPartyType = None,
        primeBPSafeId = None,
        confirmInformationDeclaration = Some(true),
        companyRegistrationNumber = Some("CRN"),
        applicantDetails = validApplicantDetails,
        bankDetails = Some(testBankDetails),
        sicAndCompliance = testSicAndCompliance.get,
        businessContact = testBusinessContact.get,
        tradingDetails = validFullTradingDetails,
        flatRateScheme = validFullFlatRateScheme.frsDetails,
        eligibilitySubmissionData = testEligibilitySubmissionData
      )
    }

    "throw an IllegalStateException when required data is missing" in {
      intercept[IllegalStateException] {
        VatSubmission.fromVatScheme(testVatScheme)
      }
    }
  }
}

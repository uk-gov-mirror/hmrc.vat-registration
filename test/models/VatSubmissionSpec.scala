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
import models.api.{BankAccount, SicAndCompliance, SicCode, VatSubmission}
import models.submission.UkCompany
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

  "converting a VatSubmission model into Json" when {
    "safe id is not present" should {
      "produce a valid Json for submission" in {
        val vatSubmission = testVatSubmission.copy(applicantDetails = validApplicantDetails.copy(bpSafeId = None))
        val json = Json.toJson(vatSubmission)(VatSubmission.submissionFormat)

        json mustBe noBpIdVatSubmissionJson
      }
    }
    "safe id is present" should {
      "produce valid json for submission with a list of customer ids" in {
        val json = Json.toJson(testVatSubmission)(VatSubmission.submissionFormat)
        val expectedJson = (JsPath \ "customerIdentification" \ "customerID")
          .prune(vatSubmissionJson.as[JsObject].deepMerge(
            Json.obj("customerIdentification" -> Json.obj(
              "primeBPSafeID" -> testBpSafeId
            ))
          )).get

        json mustBe expectedJson
      }
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

    "write a default reason why the bank account isn't provided" in {
      val vatSubmissionWithoutBank: VatSubmission = VatSubmission(
        testMessageType,
        Some(UkCompany),
        Some(true),
        Some(testCrn),
        validApplicantDetails,
        None,
        testSicAndCompliance.get,
        testBusinessContact.get,
        validFullTradingDetails,
        Some(validFullFRSDetails),
        testEligibilitySubmissionData,
        testReturns
      )

      val res = Json.toJson(vatSubmissionWithoutBank)(VatSubmission.submissionFormat)
      val expectedJson = Json.obj("reasonBankAccNotProvided" -> "1").toString()

      res.toString must include(expectedJson)
    }
  }

  "validating Json" when {
    "all sections are defined" should {
      "successfully validate a valid json submission" in {
        val expectedFrs = validFullFRSDetails.copy(businessGoods = None)

        val expected = testVatSubmission.copy(
          applicantDetails = validApplicantDetails.copy(bpSafeId = None),
          sicAndCompliance = testSicResult,
          flatRateScheme = Some(expectedFrs)
        )

        val result = Json.fromJson[VatSubmission](vatSubmissionJson)(VatSubmission.submissionFormat)

        result mustBe JsSuccess(expected)
      }
    }
    "without the compliance section" should {
      "validate successfully" in {
        val json = vatSubmissionJson.as[JsObject] - "compliance"
        val expectedFrs = validFullFRSDetails.copy(businessGoods = None)

        val expected = testVatSubmission.copy(
          applicantDetails = validApplicantDetails.copy(bpSafeId = None),
          sicAndCompliance = testSicResult.copy(labourCompliance = None),
          flatRateScheme = Some(expectedFrs)
        )

        val result = Json.fromJson[VatSubmission](json)(VatSubmission.submissionFormat)

        result mustBe JsSuccess(expected)
      }
    }
    "without the FRS section" should {
      "validate successfully" in {
        val json = (__ \ "subscription" \ "schemes").prune(vatSubmissionJson).get

        val expected = testVatSubmission.copy(
          applicantDetails = validApplicantDetails.copy(bpSafeId = None),
          sicAndCompliance = testSicResult,
          flatRateScheme = None
        )

        val result = Json.fromJson[VatSubmission](json)(VatSubmission.submissionFormat)

        result mustBe JsSuccess(expected)
      }
    }
    "without the Traders Party Type" should {
      "validate successfully" in {
        val json = (__ \ "customerIdentification" \ "tradersPartyType").prune(vatSubmissionJson).get

        val expected = testVatSubmission.copy(
          tradersPartyType = None,
          applicantDetails = validApplicantDetails.copy(bpSafeId = None),
          flatRateScheme = Some(validFullFRSDetails.copy(businessGoods = None)),
          sicAndCompliance = testSicResult
        )

        val result = Json.fromJson[VatSubmission](json)(VatSubmission.submissionFormat)

        result mustBe JsSuccess(expected)
      }
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
        eligibilitySubmissionData = Some(testEligibilitySubmissionData),
        returns = Some(testReturns.copy(zeroRatedSupplies = Some(zeroRatedSupplies)))
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

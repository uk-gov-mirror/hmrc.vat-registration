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

package services.monitoring


import enums.VatRegStatus
import fixtures.VatRegistrationFixture
import java.time.LocalDate
import helpers.VatRegSpec
import models.api.{MTDfB, VatScheme}
import play.api.libs.json.Json
import uk.gov.hmrc.http.InternalServerException



class AuditRootBlockBuilderSpec extends VatRegSpec with VatRegistrationFixture {

  val builder = app.injector.instanceOf[AuditRootBlockBuilder]

  val rootBlockTestVatScheme = VatScheme(
    id = testRegId,
    internalId = testInternalid,
    status = VatRegStatus.draft,
    eligibilitySubmissionData = Some(testEligibilitySubmissionData),
    tradingDetails = Some(validFullTradingDetails),
    applicantDetails = Some(validApplicantDetails),
    returns = Some(testReturns)
  )

  "buildRootBlock" when {
    "All required blocks are present in the vat scheme" when {
      "forward look" should {
        "return the root JSON block when BP Safe ID is missing" in {
          val res = builder.buildRootBlock(rootBlockTestVatScheme)

          res mustBe Json.obj(
            "registrationReason" -> "Forward Look",
            "registrationRelevantDate" -> "2020-10-07",
            "messageType" -> "SubscriptionSubmitted",
            "customerStatus" -> MTDfB.toString,
            "eoriRequested" -> true,
            "corporateBodyRegistered" -> Json.obj(
              "dateOfIncorporation" -> testDateOFIncorp,
              "countryOfIncorporation" -> "GB"
            ),
            "idVerificationStatus" -> "1",
            "cidVerification" -> "1",
          )
        }
        "return the root JSON block when BP Safe ID is present" in {
          val applicantDetailsWithSafeId = validApplicantDetails.copy(bpSafeId = Some(testBpSafeId))
          val res = builder.buildRootBlock(rootBlockTestVatScheme.copy(applicantDetails = Some(applicantDetailsWithSafeId)))

          res mustBe Json.obj(
            "registrationReason" -> "Forward Look",
            "registrationRelevantDate" -> "2020-10-07",
            "messageType" -> "SubscriptionSubmitted",
            "customerStatus" -> MTDfB.toString,
            "eoriRequested" -> true,
            "corporateBodyRegistered" -> Json.obj(
              "dateOfIncorporation" -> testDateOFIncorp,
              "countryOfIncorporation" -> "GB"
            ),
            "idVerificationStatus" -> "1",
            "cidVerification" -> "1",
            "businessPartnerReference" -> testBpSafeId
          )
        }
      }
      "backward look" should {
        "return the correct json" in {
          val threshold = testMandatoryThreshold.copy(
            thresholdNextThirtyDays = Some(LocalDate.of(2021, 1, 12)),
            thresholdPreviousThirtyDays = Some(LocalDate.of(2021, 1, 12)),
          )

          val eligibilityData = testEligibilitySubmissionData.copy(threshold = threshold)
          val res = builder.buildRootBlock(rootBlockTestVatScheme.copy(eligibilitySubmissionData = Some(eligibilityData)))

          res mustBe Json.obj(
            "registrationReason" -> "Backward Look",
            "registrationRelevantDate" -> "2020-12-01",
            "messageType" -> "SubscriptionSubmitted",
            "customerStatus" -> MTDfB.toString,
            "eoriRequested" -> true,
            "corporateBodyRegistered" -> Json.obj(
              "dateOfIncorporation" -> testDateOFIncorp,
              "countryOfIncorporation" -> "GB"
            ),
            "idVerificationStatus" -> "1",
            "cidVerification" -> "1"
          )
        }
      }
      "registering voluntarily" should {
        "return the correct json" in {
          val eligibilityData = testEligibilitySubmissionData.copy(threshold = testVoluntaryThreshold)
          val res = builder.buildRootBlock(rootBlockTestVatScheme.copy(eligibilitySubmissionData = Some(eligibilityData)))

          res mustBe Json.obj(
            "registrationReason" -> "Voluntary",
            "registrationRelevantDate" -> "2018-01-01",
            "messageType" -> "SubscriptionSubmitted",
            "customerStatus" -> MTDfB.toString,
            "eoriRequested" -> true,
            "corporateBodyRegistered" -> Json.obj(
              "dateOfIncorporation" -> testDateOFIncorp,
              "countryOfIncorporation" -> "GB"
            ),
            "idVerificationStatus" -> "1",
            "cidVerification" -> "1"
          )
        }
      }
    }
    "the eligibility submission data block is missing in the vat scheme" should {
      "throw an exception" in {
        intercept[InternalServerException] {
          builder.buildRootBlock(rootBlockTestVatScheme.copy(eligibilitySubmissionData = None))
        }
      }
    }
    "the trading details block is missing in the vat scheme" should {
      "throw an exception" in {
        intercept[InternalServerException] {
          builder.buildRootBlock(rootBlockTestVatScheme.copy(tradingDetails = None))
        }
      }
    }
    "the applicant details block is missing in the vat scheme" should {
      "throw an exception" in {
        intercept[InternalServerException] {
          builder.buildRootBlock(rootBlockTestVatScheme.copy(applicantDetails = None))
        }
      }
    }
    "the returns block is missing in the vat scheme" should {
      "throw an exception" in {
        intercept[InternalServerException] {
          builder.buildRootBlock(rootBlockTestVatScheme.copy(returns = None))
        }
      }
    }
  }

}

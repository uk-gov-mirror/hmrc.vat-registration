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

package services.submission

import fixtures.{VatRegistrationFixture, VatSubmissionFixture}
import helpers.VatRegSpec
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import play.api.libs.json.{JsObject, Json}
import play.api.test.Helpers._
import services.submission.buildermocks.{MockAdminBlockBuilder, MockContactBlockBuilder, MockCustomerIdentificationBlockBuilder, MockPeriodsBlockBuilder}
import uk.gov.hmrc.http.InternalServerException

import scala.concurrent.Future

class SubmissionPayloadBuilderSpec extends VatRegSpec
  with VatRegistrationFixture
  with VatSubmissionFixture
  with MockAdminBlockBuilder
  with MockCustomerIdentificationBlockBuilder
  with MockContactBlockBuilder
  with MockPeriodsBlockBuilder
{

  object TestBuilder extends SubmissionPayloadBuilder(
    mockRegistrationMongoRepository,
    mockAdminBlockBuilder,
    mockCustomerIdentificationBlockBuilder,
    mockContactBlockBuilder,
    mockPeriodsBlockBuilder
  )

  val testAdminBlockJson: JsObject = Json.obj(
    "additionalInformation" -> Json.obj(
      "customerStatus" -> "2"
    ),
    "attachments" -> Json.obj(
      "EORIrequested" -> true
    )
  )

  val testCustomerIdentificationBlockJson: JsObject = Json.obj(
    "tradingName" -> "trading-name",
    "tradersPartyType" -> "50",
    "primeBPSafeID" -> "testBpSafeId",
    "shortOrgName" -> "testCompanyName"
  )

  val testContactBlockJson: JsObject = Json.obj(
    "address" -> Json.obj(
      "line1" -> "line1",
      "line2" -> "line2",
      "postCode" -> "ZZ1 1ZZ",
      "countryCode" -> "GB"
    ),
    "commDetails" -> Json.obj(
      "telephone" -> "12345",
      "mobileNumber" -> "54321",
      "email" -> "email@email.com",
      "commsPreference" -> "ZEL"
    )
  )

  val testPeriodsBlockJson: JsObject = Json.obj("customerPreferredPeriodicity" -> "MM")

  val expectedJson: JsObject = Json.obj(
    "admin" -> testAdminBlockJson,
    "customerIdentification" -> testCustomerIdentificationBlockJson,
    "contact" -> testContactBlockJson,
    "subscription" -> Json.obj(
      "reasonForSubscription" -> Json.obj(
        "registrationReason" -> "0016",
        "relevantDate" -> "2020-10-07",
        "voluntaryOrEarlierDate" -> "2018-01-01",
        "exemptionOrException" -> "0"
      ),
      "corporateBodyRegistered" -> Json.obj(
        "companyRegistrationNumber" -> "testCrn",
        "dateOfIncorporation" -> "2020-01-02",
        "countryOfIncorporation" -> "GB"
      ),
      "businessActivities" -> Json.obj(
        "description" -> "this is my business description",
        "SICCodes" -> Json.obj(
          "primaryMainCode" -> "12345"
        )
      ),
      "yourTurnover" -> Json.obj(
        "turnoverNext12Months" -> "",
        "zeroRatedSupplies" -> 12.99,
        "VATRepaymentExpected" -> true
      ),
      "schemes" -> Json.obj(
        "FRSCategory" -> "",
        "FRSPercentage" -> "",
        "startDate" -> "",
        "limitedCostTrader" -> ""
      )
    ),
    "periods" -> testPeriodsBlockJson
  )

  "buildSubmissionPayload" should {
    "return a submission json object" when {
      "all required pieces of data are available in the database" in {
        mockBuildAdminBlock(testRegId)(Future.successful(testAdminBlockJson))

        mockBuildCustomerIdentificationBlock(testRegId)(Future.successful(testCustomerIdentificationBlockJson))

        mockBuildContactBlock(testRegId)(Future.successful(testContactBlockJson))

        when(mockRegistrationMongoRepository.fetchEligibilitySubmissionData(ArgumentMatchers.eq(testRegId)))
          .thenReturn(Future.successful(Some(testEligibilitySubmissionData)))

        when(mockRegistrationMongoRepository.fetchReturns(ArgumentMatchers.eq(testRegId)))
          .thenReturn(Future.successful(Some(testReturns)))

        when(mockRegistrationMongoRepository.getApplicantDetails(ArgumentMatchers.eq(testRegId)))
          .thenReturn(Future.successful(Some(validApplicantDetails)))

        when(mockRegistrationMongoRepository.fetchSicAndCompliance(ArgumentMatchers.eq(testRegId)))
          .thenReturn(Future.successful(testSicAndCompliance))

        mockBuildPeriodsBlock(testRegId)(Future.successful(testPeriodsBlockJson))

        val result = await(TestBuilder.buildSubmissionPayload(testRegId))

        result mustBe expectedJson

      }
    }

    "throw an exception" when {
      "one of the required pieces of data is available in the database" in {
        mockBuildAdminBlock(testRegId)(Future.failed(new InternalServerException("Data not in database")))

        intercept[InternalServerException] {
          await(TestBuilder.buildSubmissionPayload(testRegId))
        }
      }
    }
  }

}

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
import models.api.{BvCtEnrolled, BvPass, BvUnchallenged, FailedStatus}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.libs.json.{JsObject, Json}
import play.api.test.Helpers._

import scala.concurrent.Future

class CustomerIdentificationBlockBuilderSpec extends VatRegSpec with VatRegistrationFixture with VatSubmissionFixture {

  class Setup {
    val service: CustomerIdentificationBlockBuilder = new CustomerIdentificationBlockBuilder(
      registrationMongoRepository = mockRegistrationMongoRepository
    )
  }

  lazy val customerIdentificationBlockWithBPJson: JsObject = Json.parse(
    """
      |{
      |    "tradingName": "trading-name",
      |    "tradersPartyType": "50",
      |    "primeBPSafeID": "testBpSafeId",
      |    "shortOrgName": "testCompanyName"
      |}
      |""".stripMargin).as[JsObject]

  def customerIdentificationBlockJson(idVerificationStatusCode: Int): JsObject = Json.parse(
    s"""
       |{
       |    "tradingName": "trading-name",
       |    "tradersPartyType": "50",
       |    "shortOrgName": "testCompanyName",
       |    "customerID": [
       |      {
       |        "idValue": "testCtUtr",
       |        "idType": "UTR",
       |        "IDsVerificationStatus": "$idVerificationStatusCode"
       |      },
       |      {
       |        "idValue": "testCrn",
       |        "idType": "CRN",
       |        "IDsVerificationStatus": "$idVerificationStatusCode",
       |        "date": "2020-01-02"
       |      }
       |    ]
       |}
       |""".stripMargin).as[JsObject]

  "buildCustomerIdentificationBlock" should {
    "return Status Code 1" when {
      "the businessVerificationStatus is BvPass" in new Setup {
        when(mockRegistrationMongoRepository.getApplicantDetails(any()))
          .thenReturn(Future.successful(Some(validApplicantDetails.copy(identifiersMatch = Some(true), businessVerification = Some(BvPass), registration = Some(FailedStatus)))))

        when(mockRegistrationMongoRepository.retrieveTradingDetails(any()))
          .thenReturn(Future.successful(Some(validFullTradingDetails)))

        val result: JsObject = await(service.buildCustomerIdentificationBlock(testRegId))
        result mustBe customerIdentificationBlockJson(1)
      }
      "the businessVerificationStatus is CtEnrolled" in new Setup {
        when(mockRegistrationMongoRepository.getApplicantDetails(any()))
          .thenReturn(Future.successful(Some(validApplicantDetails.copy(identifiersMatch = Some(true), businessVerification = Some(BvCtEnrolled), registration = Some(FailedStatus)))))

        when(mockRegistrationMongoRepository.retrieveTradingDetails(any()))
          .thenReturn(Future.successful(Some(validFullTradingDetails)))

        val result: JsObject = await(service.buildCustomerIdentificationBlock(testRegId))
        result mustBe customerIdentificationBlockJson(1)
      }
    }
    "return Status Code 2" when {
      "the identifiersMatch is false" in new Setup {
        when(mockRegistrationMongoRepository.getApplicantDetails(any()))
          .thenReturn(Future.successful(Some(validApplicantDetails.copy(identifiersMatch = Some(false), businessVerification = Some(BvUnchallenged)))))

        when(mockRegistrationMongoRepository.retrieveTradingDetails(any()))
          .thenReturn(Future.successful(Some(validFullTradingDetails)))

        val result: JsObject = await(service.buildCustomerIdentificationBlock(testRegId))
        result mustBe customerIdentificationBlockJson(2)
      }
    }
    "return Status Code 3" when {
      "businessVerification fails" in new Setup {
        when(mockRegistrationMongoRepository.getApplicantDetails(any()))
          .thenReturn(Future.successful(Some(validApplicantDetails)))

        when(mockRegistrationMongoRepository.retrieveTradingDetails(any()))
          .thenReturn(Future.successful(Some(validFullTradingDetails)))

        val result: JsObject = await(service.buildCustomerIdentificationBlock(testRegId))
        result mustBe customerIdentificationBlockJson(3)
      }
      "businessVerification is not called" in new Setup {
        when(mockRegistrationMongoRepository.getApplicantDetails(any()))
          .thenReturn(Future.successful(Some(validApplicantDetails.copy(businessVerification = Some(BvUnchallenged)))))

        when(mockRegistrationMongoRepository.retrieveTradingDetails(any()))
          .thenReturn(Future.successful(Some(validFullTradingDetails)))

        val result: JsObject = await(service.buildCustomerIdentificationBlock(testRegId))
        result mustBe customerIdentificationBlockJson(3)
      }
    }
    "return the BP Safe ID" when {
      "businessVerificationStatus is Pass" in new Setup {
        when(mockRegistrationMongoRepository.getApplicantDetails(any()))
          .thenReturn(Future.successful(Some(validApplicantDetails.copy(bpSafeId = Some(testBpSafeId), businessVerification = Some(BvPass)))))

        when(mockRegistrationMongoRepository.retrieveTradingDetails(any()))
          .thenReturn(Future.successful(Some(validFullTradingDetails)))

        val result: JsObject = await(service.buildCustomerIdentificationBlock(testRegId))
        result mustBe customerIdentificationBlockWithBPJson
      }
      "businessVerification is CT-Enrolled" in new Setup {
        when(mockRegistrationMongoRepository.getApplicantDetails(any()))
          .thenReturn(Future.successful(Some(validApplicantDetails.copy(bpSafeId = Some(testBpSafeId), businessVerification = Some(BvCtEnrolled)))))

        when(mockRegistrationMongoRepository.retrieveTradingDetails(any()))
          .thenReturn(Future.successful(Some(validFullTradingDetails)))

        val result: JsObject = await(service.buildCustomerIdentificationBlock(testRegId))
        result mustBe customerIdentificationBlockWithBPJson
      }
    }
  }

}

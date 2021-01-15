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

import models.api.EligibilitySubmissionData.voluntaryKey
import play.api.libs.json.{JsObject, Json}
import repositories.RegistrationMongoRepository
import uk.gov.hmrc.http.InternalServerException
import utils.JsonUtils._

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SubmissionPayloadBuilder @Inject()(registrationMongoRepository: RegistrationMongoRepository,
                                         adminBlockBuilder: AdminBlockBuilder,
                                         customerIdentificationBlockBuilder: CustomerIdentificationBlockBuilder,
                                         contactBlockBuilder: ContactBlockBuilder,
                                         periodsBlockBuilder: PeriodsBlockBuilder,
                                         bankDetailsBlockBuilder: BankDetailsBlockBuilder,
                                         complianceBlockBuilder: ComplianceBlockBuilder
                                        )(implicit ec: ExecutionContext) {

  def buildSubmissionPayload(regId: String): Future[JsObject] = for {
    adminBlock <- adminBlockBuilder.buildAdminBlock(regId)
    customerIdentificationBlock <- customerIdentificationBlockBuilder.buildCustomerIdentificationBlock(regId)
    contactBlock <- contactBlockBuilder.buildContactBlock(regId)
    subscriptionBlock <- buildSubscriptionBlock(regId)
    periodsBlock <- periodsBlockBuilder.buildPeriodsBlock(regId)
    complianceBlock <- complianceBlockBuilder.buildComplianceBlock(regId)
    bankDetailsBlock <- bankDetailsBlockBuilder.buildBankDetailsBlock(regId)
  } yield jsonObject(
    "admin" -> adminBlock,
    "customerIdentification" -> customerIdentificationBlock,
    "contact" -> contactBlock,
    "subscription" -> subscriptionBlock,
    "periods" -> periodsBlock,
    "bankDetails" -> bankDetailsBlock,
    optional("compliance" -> complianceBlock)
  )

  private def buildSubscriptionBlock(regId: String): Future[JsObject] = for {
    optEligibilityData <- registrationMongoRepository.fetchEligibilitySubmissionData(regId)
    optReturns <- registrationMongoRepository.fetchReturns(regId)
    optApplicantDetails <- registrationMongoRepository.getApplicantDetails(regId)
    optSicAndCompliance <- registrationMongoRepository.fetchSicAndCompliance(regId)
  } yield (optEligibilityData, optReturns, optApplicantDetails, optSicAndCompliance) match {
    case (Some(eligibilityData), Some(returns), Some(applicantDetails), Some(sicAndCompliance)) => Json.obj(
      "reasonForSubscription" -> jsonObject(
        "registrationReason" -> eligibilityData.reasonForRegistration,
        optional("relevantDate" -> {
          if (eligibilityData.reasonForRegistration != voluntaryKey) {
            Some(eligibilityData.earliestDate)
          } else {
            returns.start.date
          }
        }),
        optional("voluntaryOrEarlierDate" -> returns.start.date),
        "exemptionOrException" -> eligibilityData.exceptionOrExemption
      ),
      "corporateBodyRegistered" -> jsonObject(
        optional("companyRegistrationNumber" -> applicantDetails.companyNumber),
        "dateOfIncorporation" -> applicantDetails.dateOfIncorporation,
        "countryOfIncorporation" -> applicantDetails.countryOfIncorporation
      ),
      "businessActivities" -> Json.obj(
        "description" -> sicAndCompliance.businessDescription,
        "SICCodes" -> Json.obj(
          "primaryMainCode" -> sicAndCompliance.mainBusinessActivity.id
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
    )
    case _ =>
      throw new InternalServerException("Could not build subscription block for submission due to missing data")
  }

}

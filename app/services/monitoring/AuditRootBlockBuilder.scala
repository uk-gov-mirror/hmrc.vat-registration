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

import models.api.{EligibilitySubmissionData, Threshold, VatScheme}
import play.api.libs.json.{JsObject, JsValue, Json}
import uk.gov.hmrc.http.InternalServerException
import utils.JsonUtils.{jsonObject, optional}

import javax.inject.Singleton


@Singleton
class AuditRootBlockBuilder {

  private val messageType = "SubscriptionSubmitted"
  private val idVerificationStatus: String = "1"
  private val cidVerificationStatus: String = "1"

  def buildRootBlock(vatScheme: VatScheme): JsObject = {
    (vatScheme.eligibilitySubmissionData, vatScheme.tradingDetails, vatScheme.applicantDetails, vatScheme.returns) match {
      case (None, _, _, _) =>
        throw new InternalServerException("[AdminBlockBuilder] Vat scheme missing Eligibility data")
      case (_, None, _, _) =>
        throw new InternalServerException("[AdminBlockBuilder] Vat scheme missing Trading details")
      case (_, _, None, _) =>
        throw new InternalServerException("[AdminBlockBuilder] Vat scheme missing Applicant details")
      case (_, _, _, None) =>
        throw new InternalServerException("[AdminBlockBuilder] Vat scheme missing Returns")
      case (Some(eligibilityData), Some(tradingDetails), Some(applicantDetails), Some(returns)) =>
        jsonObject(
          "registrationReason" -> eligibilityData.reasonForRegistration(humanReadable = true),
          optional("registrationRelevantDate" -> {
            if (eligibilityData.reasonForRegistration() == EligibilitySubmissionData.voluntaryKey)
              returns.start.date
            else
              Some(eligibilityData.earliestDate)
          }),
          "messageType" -> messageType,
          "customerStatus" -> eligibilityData.customerStatus.toString,
          "eoriRequested" -> tradingDetails.eoriRequested,
          "corporateBodyRegistered" -> Json.obj(
            "dateOfIncorporation" -> applicantDetails.dateOfIncorporation,
            "countryOfIncorporation" -> applicantDetails.countryOfIncorporation
          ),
          "idVerificationStatus" -> idVerificationStatus,
          "cidVerification" -> cidVerificationStatus,
          optional("businessPartnerReference" -> applicantDetails.bpSafeId)
        )
    }
  }

}

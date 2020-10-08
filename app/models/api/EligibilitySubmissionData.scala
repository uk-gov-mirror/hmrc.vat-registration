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

package models.api

import play.api.libs.functional.syntax._
import play.api.libs.json._
import uk.gov.hmrc.http.InternalServerException

case class EligibilitySubmissionData(threshold: Threshold,
                                     exceptionOrExemption: String,
                                     estimates: TurnoverEstimates,
                                     customerStatus: CustomerStatus)

object EligibilitySubmissionData {

  val exceptionKey = "2"
  val exemptionKey = "1"
  val nonExceptionOrExemptionKey = "0"

  val submissionFormat: Format[EligibilitySubmissionData] = (
    (__ \ "subscription" \ "reasonForSubscription").format[Threshold](Threshold.submissionFormat) and
    (__ \ "subscription" \ "reasonForSubscription" \ "exemptionOrException").format[String] and
    (__ \ "subscription" \ "yourTurnover" \ "turnoverNext12Months").format[TurnoverEstimates](TurnoverEstimates.submissionFormat) and
    (__ \ "admin" \ "additionalInformation" \ "customerStatus").format[CustomerStatus](CustomerStatus.format)
    ) (EligibilitySubmissionData.apply, unlift(EligibilitySubmissionData.unapply))

  val eligibilityReads: Reads[EligibilitySubmissionData] = Reads { json =>
    (
      json.validate[Threshold](Threshold.eligibilityDataJsonReads) and
      (
        (json \ "vatRegistrationException").validateOpt[Boolean] and
        (json \ "vatExemption").validateOpt[Boolean]
      ) ((exception, exemption) =>
        (exception.contains(true), exemption.contains(true)) match {
          case (excepted, exempt) if !excepted && !exempt => nonExceptionOrExemptionKey
          case (excepted, _) if excepted => exceptionKey
          case (_, exempt) if exempt => exemptionKey
          case (_, _) =>
            throw new InternalServerException("[EligibilitySubmissionData][eligibilityReads] eligibility returned invalid exception/exemption data")
        }
      ) and
      json.validate[TurnoverEstimates](TurnoverEstimates.eligibilityDataJsonReads) and
      json.validate[CustomerStatus](CustomerStatus.eligibilityDataJsonReads)
    ) (EligibilitySubmissionData.apply _)
  }

  implicit val format: Format[EligibilitySubmissionData] = Json.format[EligibilitySubmissionData]
}
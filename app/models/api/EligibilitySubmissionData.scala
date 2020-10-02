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
import play.api.libs.json.{Format, Json, Reads}

case class EligibilitySubmissionData(threshold: Threshold,
                                     estimates: TurnoverEstimates,
                                     customerStatus: CustomerStatus)

object EligibilitySubmissionData {

  val eligibilityReads: Reads[EligibilitySubmissionData] = Reads { json =>
    (
      json.validate[Threshold](Threshold.eligibilityDataJsonReads) and
      json.validate[TurnoverEstimates](TurnoverEstimates.eligibilityDataJsonReads) and
      json.validate[CustomerStatus](CustomerStatus.eligibilityDataJsonReads)
    ) (EligibilitySubmissionData.apply _)
  }

  implicit val format: Format[EligibilitySubmissionData] = Json.format[EligibilitySubmissionData]
}
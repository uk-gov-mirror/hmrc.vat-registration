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

import java.time.LocalDate

import play.api.libs.json._

case class StartDate(date: Option[LocalDate])

object StartDate {
  implicit val format: OFormat[StartDate] = Json.format[StartDate]
}

case class Returns(reclaimVatOnMostReturns: Boolean,
                   frequency: String,
                   staggerStart: Option[String],
                   start: StartDate)

object Returns extends VatAccountingPeriodValidator {
  implicit val format: Format[Returns] = Json.format[Returns]
}

case class TurnoverEstimates(turnoverEstimate: Long)

object TurnoverEstimates {

  val eligibilityDataJsonReads: Reads[TurnoverEstimates] = new Reads[TurnoverEstimates] {
    override def reads(json: JsValue): JsResult[TurnoverEstimates] = {
      (json \ "turnoverEstimate-value").validate[Long]
        .map(turnOverEstimateAmount => TurnoverEstimates(turnoverEstimate = turnOverEstimateAmount))
    }
  }

  implicit val format: Format[TurnoverEstimates] = Json.format
}

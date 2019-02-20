/*
 * Copyright 2019 HM Revenue & Customs
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
import play.api.libs.functional.syntax._


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

case class TurnoverEstimates(@deprecated("Use turnoverEstimate instead", "SCRS-11579") vatTaxable: Option[Long] = None,
                             turnoverEstimate: Option[Long] = None) //TODO: Once VAT FE is refactored, change the model to a mandatory turnoverEstimate

object TurnoverEstimates {

  val eligibilityDataJsonReads = new Reads[TurnoverEstimates] {
    override def reads(json: JsValue): JsResult[TurnoverEstimates] = {
      (json \ "turnoverEstimate-value").validate[String] flatMap { turnOverEnum =>
        val amount = turnOverEnum match {
          case "tenthousand"        => (json \ "turnoverEstimate-optionalData").validate[Long]
          case "zeropounds"         => JsSuccess(0L)
          case "oneandtenthousand"  => JsSuccess(10000L)
        }
        amount.map(turnOverEstimateAmount => TurnoverEstimates(turnoverEstimate = Some(turnOverEstimateAmount)))
      }
    }
  }

  implicit val format: Format[TurnoverEstimates] = Json.format
}

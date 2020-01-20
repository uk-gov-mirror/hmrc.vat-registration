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

import play.api.libs.json.{JsError, JsResult, JsSuccess, JsValue, Json, OFormat, Reads}

import scala.collection.Seq

case class Threshold(mandatoryRegistration: Boolean,
                     @deprecated("Not required anymore", "SCRS-11579") voluntaryReason: Option[String] = None,
                     @deprecated("Not required anymore", "SCRS-11579") overThresholdDateThirtyDays: Option[LocalDate] = None,
                     @deprecated("Use thresholdPreviousThirtyDays instead", "SCRS-11579") pastOverThresholdDateThirtyDays: Option[LocalDate] = None,
                     @deprecated("Use thresholdInTwelveMonths instead", "SCRS-11579") overThresholdOccuredTwelveMonth: Option[LocalDate] = None,
                     thresholdPreviousThirtyDays: Option[LocalDate] = None,
                     thresholdInTwelveMonths: Option[LocalDate] = None)

object Threshold {
  implicit val format: OFormat[Threshold] = Json.format

  val eligibilityDataJsonReads: Reads[Threshold] = new Reads[Threshold] {
    override def reads(json: JsValue): JsResult[Threshold] = {
      val voluntaryRegistration = (json \ "voluntaryRegistration").validateOpt[Boolean]
      val thresholdThirtyDays = (json \ "thresholdPreviousThirtyDays-optionalData").validateOpt[LocalDate]
      val thresholdTwelveMonths = (json \ "thresholdInTwelveMonths-optionalData").validateOpt[LocalDate]

      val seqErrors = voluntaryRegistration.fold(identity, _ => Seq.empty) ++
        thresholdThirtyDays.fold(identity, _ => Seq.empty) ++
        thresholdTwelveMonths.fold(identity, _ => Seq.empty)

      if(seqErrors.nonEmpty) {
        JsError(seqErrors)
      } else {
        JsSuccess(Threshold(
          mandatoryRegistration = !voluntaryRegistration.get.contains(true) && List(thresholdThirtyDays.get, thresholdTwelveMonths.get).flatten.nonEmpty,
          thresholdPreviousThirtyDays = thresholdThirtyDays.get,
          thresholdInTwelveMonths = thresholdTwelveMonths.get
        ))
      }
    }
  }
}

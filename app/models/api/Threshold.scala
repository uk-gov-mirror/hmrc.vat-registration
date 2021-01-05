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

package models.api

import java.time.LocalDate

import play.api.libs.json._
import uk.gov.hmrc.http.InternalServerException

case class Threshold(mandatoryRegistration: Boolean,
                     thresholdPreviousThirtyDays: Option[LocalDate] = None,
                     thresholdInTwelveMonths: Option[LocalDate] = None,
                     thresholdNextThirtyDays: Option[LocalDate] = None)

object Threshold {

  val voluntaryKey = "0018"
  val forwardLookKey = "0016"
  val backwardLookKey = "0015"

  val submissionReads: Reads[Threshold] = Reads { json =>
    (
      (json \ "registrationReason").validate[String],
      (json \ "relevantDate").validateOpt[LocalDate]
    ) match {
      case (JsSuccess(`voluntaryKey`, _), _) =>
        JsSuccess(Threshold(mandatoryRegistration = false))
      case (JsSuccess(`forwardLookKey`, _), JsSuccess(date, _)) =>
        JsSuccess(Threshold(mandatoryRegistration = true, date, date, date))
      case (JsSuccess(`backwardLookKey`, _), JsSuccess(date, _)) =>
        JsSuccess(Threshold(mandatoryRegistration = true, date, date, date))
      case _ => throw new InternalServerException("[Threshold][submissionReads] could not parse the Threshold block")
    }
  }

  val submissionWrites: Writes[Threshold] = Writes {
    case Threshold(false, _, _, _) => Json.obj(
      "registrationReason" -> voluntaryKey
    )
    case Threshold(true, forwardLook1, backwardLook, forwardLook2) =>
      val earliestDate = Seq(
        forwardLook1,
        backwardLook.map(_.withDayOfMonth(1).plusMonths(2)),
        forwardLook2
      ).flatten.min(Ordering.by((date: LocalDate) => date.toEpochDay))

      if (forwardLook1.contains(earliestDate) || forwardLook2.contains(earliestDate)) {
        Json.obj(
          "registrationReason" -> forwardLookKey,
          "relevantDate" -> earliestDate
        )
      }
      else {
        Json.obj(
          "registrationReason" -> backwardLookKey,
          "relevantDate" -> earliestDate
        )
      }
  }

  val submissionFormat: Format[Threshold] = Format(submissionReads, submissionWrites)

  val eligibilityDataJsonReads: Reads[Threshold] = Reads { json =>
    (
      (json \ "voluntaryRegistration").validateOpt[Boolean],
      (json \ "thresholdPreviousThirtyDays-optionalData").validateOpt[LocalDate],
      (json \ "thresholdInTwelveMonths-optionalData").validateOpt[LocalDate],
      (json \ "thresholdNextThirtyDays-optionalData").validateOpt[LocalDate]
    ) match {
      case (JsSuccess(voluntaryRegistration, _), JsSuccess(thresholdPreviousThirtyDays, _),
      JsSuccess(thresholdInTwelveMonths, _), JsSuccess(thresholdNextThirtyDays, _)) =>
        if (!voluntaryRegistration.getOrElse(false) &
          Seq(thresholdInTwelveMonths, thresholdNextThirtyDays, thresholdPreviousThirtyDays).flatten.isEmpty) {
          throw new InternalServerException("[Threshold][eligibilityDataJsonReads] mandatory user missing thresholds")
        }

        JsSuccess(Threshold(
          !voluntaryRegistration.getOrElse(false),
          thresholdPreviousThirtyDays,
          thresholdInTwelveMonths,
          thresholdNextThirtyDays
        ))
      case (voluntaryRegistration, thresholdPreviousThirtyDays, thresholdInTwelveMonths, thresholdNextThirtyDays) =>
        val seqErrors = voluntaryRegistration.fold(identity, _ => Seq.empty) ++
          thresholdPreviousThirtyDays.fold(identity, _ => Seq.empty) ++
          thresholdInTwelveMonths.fold(identity, _ => Seq.empty) ++
          thresholdNextThirtyDays.fold(identity, _ => Seq.empty)

        JsError(seqErrors)
    }
  }

  implicit val format: OFormat[Threshold] = Json.format

}

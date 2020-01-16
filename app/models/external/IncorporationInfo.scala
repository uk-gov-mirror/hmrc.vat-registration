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

package models.external

import java.time.{Instant, LocalDate, ZoneId}

import org.joda.time.DateTime
import play.api.libs.functional.syntax._
import play.api.libs.json._


case class IncorpSubscription(transactionId: String, regime: String, subscriber: String, callbackUrl: String)

object IncorpSubscription {
  implicit val format = Json.format[IncorpSubscription]

  val iiReads: Reads[IncorpSubscription] = {
    val keyPath: JsPath = __ \ "IncorpSubscriptionKey"

    (
      (keyPath \ "transactionId").read[String] and
      (keyPath \ "discriminator").read[String] and
      (keyPath \ "subscriber").read[String] and
      (__ \ "SCRSIncorpSubscription" \ "callbackUrl").read[String]
    )(IncorpSubscription.apply _)
  }
}

case class IncorpStatusEvent(status: String, crn: Option[String], incorporationDate: Option[LocalDate], description: Option[String])

object IncorpStatusEvent {
  implicit val format = Json.format[IncorpStatusEvent]

  val localDateReads = Reads[LocalDate](_.validate[Long].map[LocalDate](Instant.ofEpochMilli(_).atZone(ZoneId.of("Europe/London")).toLocalDate()))

  val iiReads: Reads[IncorpStatusEvent] = (
    (__ \ "status").read[String] and
    (__ \ "crn").readNullable[String] and
    (__ \ "incorporationDate").readNullable[LocalDate](localDateReads) and
    (__ \ "description").readNullable[String]
  )(IncorpStatusEvent.apply _)
}

case class IncorporationStatus(subscription: IncorpSubscription, statusEvent: IncorpStatusEvent)

object IncorporationStatus {
  implicit val format = Json.format[IncorporationStatus]

  val iiReads: Reads[IncorporationStatus] = {
    val root: JsPath = __ \ "SCRSIncorpStatus"

    (root.read[IncorpSubscription](IncorpSubscription.iiReads) and
    (root \ "IncorpStatusEvent").read[IncorpStatusEvent](IncorpStatusEvent.iiReads)
    )(IncorporationStatus.apply _)
  }
}

case class IncorpStatus(transactionId: String,
                        status: String,
                        crn: Option[String],
                        description: Option[String],
                        incorporationDate: Option[DateTime]){

//  def toIncorpUpdate: IncorpUpdate = {
//    IncorpUpdate(transactionId, status, crn, incorporationDate, "N/A", description)
//  }
}

object IncorpStatus {
  implicit val reads = (
    ( __ \ "SCRSIncorpStatus" \ "IncorpSubscriptionKey" \ "transactionId").read[String] and
      ( __ \ "SCRSIncorpStatus" \ "IncorpStatusEvent" \ "status").read[String] and
      ( __ \ "SCRSIncorpStatus" \ "IncorpStatusEvent" \ "crn").readNullable[String] and
      ( __ \ "SCRSIncorpStatus" \ "IncorpStatusEvent" \ "description").readNullable[String] and
      ( __ \ "SCRSIncorpStatus" \ "IncorpStatusEvent" \ "incorporationDate").readNullable[DateTime]
    )(IncorpStatus.apply _)
}

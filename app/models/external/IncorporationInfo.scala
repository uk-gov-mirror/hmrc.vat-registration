/*
 * Copyright 2017 HM Revenue & Customs
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

import org.joda.time.DateTime
import play.api.libs.functional.syntax._
import play.api.libs.json._


final case class IncorpSubscription(transactionId: String, regime: String, subscriber: String, callbackUrl: String)

object IncorpSubscription {

  implicit val format = Json.format[IncorpSubscription]

  val iiReads: Reads[IncorpSubscription] = {

    val keyPath: JsPath = __ \ "IncorpSubscriptionKey"

    ((keyPath \ "transactionId").read[String] and
      (keyPath \ "discriminator").read[String] and
      (keyPath \ "subscriber").read[String] and
      (__ \ "SCRSIncorpSubscription" \ "callbackUrl").read[String]
      ) (IncorpSubscription.apply _)

  }

}

final case class IncorpStatusEvent(status: String, crn: Option[String], incorporationDate: Option[DateTime], description: Option[String], timestamp: DateTime)

object IncorpStatusEvent {

  implicit val format = Json.format[IncorpStatusEvent]

  val iiReads: Reads[IncorpStatusEvent] =
    ((__ \ "status").read[String] and
      (__ \ "crn").readNullable[String] and
      (__ \ "incorporationDate").readNullable[DateTime] and
      (__ \ "description").readNullable[String] and
      (__ \ "timestamp").read[DateTime]
      ) (IncorpStatusEvent.apply _)

}

final case class IncorporationStatus(subscription: IncorpSubscription, statusEvent: IncorpStatusEvent)

object IncorporationStatus {

  implicit val format = Json.format[IncorporationStatus]

  val iiReads: Reads[IncorporationStatus] = {
    val root: JsPath = __ \ "SCRSIncorpStatus"

    (root.read[IncorpSubscription](IncorpSubscription.iiReads) and
      (root \ "IncorpStatusEvent").read[IncorpStatusEvent](IncorpStatusEvent.iiReads)
      ) (IncorporationStatus.apply _)
  }

}

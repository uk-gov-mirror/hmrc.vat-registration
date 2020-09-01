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

case class VatSubmission(messageType: String = "SubscriptionCreate",
                         customerStatus: Option[String],
                         tradersPartyType: Option[String],
                         primeBPSafeId: Option[String],
                         address: Option[Address],
                         confirmInformationDeclaration: Option[Boolean])

object VatSubmission {

  val submissionFormat: OFormat[VatSubmission] = (
    (__ \ "messageType").format[String] and
      (__ \ "admin" \ "additionalInformation" \ "customerStatus").formatNullable[String] and
      (__ \ "customerIdentification" \ "tradersPartyType").formatNullable[String] and
      (__ \ "customerIdentification" \ "primeBPSafeId").formatNullable[String] and
      (__ \ "contact" \ "address").formatNullable[Address](Address.submissionFormat) and
      (__ \ "declaration" \ "declarationSigning" \ "confirmInformationDeclaration").formatNullable[Boolean]
    ) (VatSubmission.apply, unlift(VatSubmission.unapply))

  implicit val mongoFormat: OFormat[VatSubmission] = Json.format[VatSubmission]
}

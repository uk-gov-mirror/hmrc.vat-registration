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

case class Address(line1: String,
                   line2: String,
                   line3: Option[String] = None,
                   line4: Option[String] = None,
                   postcode: Option[String] = None,
                   country: Option[String] = None)

object Address {

  val submissionFormat: OFormat[Address] = (
    (__ \ "line1").format[String] and
      (__ \ "line2").format[String] and
      (__ \ "line3").formatNullable[String] and
      (__ \ "line4").formatNullable[String] and
      (__ \ "postCode").formatNullable[String] and
      (__ \ "countryCode").formatNullable[String]
    ) (Address.apply, unlift(Address.unapply))

  implicit val format: OFormat[Address] = Json.format[Address]
}

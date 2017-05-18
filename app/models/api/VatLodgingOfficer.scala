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

package models.api

import play.api.libs.functional.syntax._
import play.api.libs.json._

case class VatLodgingOfficer(currentAddress: ScrsAddress, dob: DateOfBirth, nino: String, role: String)

object VatLodgingOfficer extends VatLodgingOfficerValidator {

  implicit val format = (
    (__ \ "currentAddress").format[ScrsAddress] and
      (__ \ "dob").format[DateOfBirth] and
      (__ \ "nino").format[String](ninoValidator) and
      (__ \ "role").format[String](roleValidator)
    ) (VatLodgingOfficer.apply, unlift(VatLodgingOfficer.unapply))

}

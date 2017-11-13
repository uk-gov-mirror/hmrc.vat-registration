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

case class VatLodgingOfficer(currentAddress: Option[ScrsAddress] = None,
                             dob: Option[DateOfBirth] = None,
                             nino: Option[String] = None,
                             role: Option[String] = None,
                             name: Option[Name] = None,
                             changeOfName: Option[ChangeOfName] = None,
                             currentOrPreviousAddress : Option[CurrentOrPreviousAddress] = None,
                             contact: Option[OfficerContactDetails] = None,
                             ivPassed: Boolean = false)

object VatLodgingOfficer extends VatLodgingOfficerValidator {

  implicit val format = (
    (__ \ "currentAddress").formatNullable[ScrsAddress] and
    (__ \ "dob").formatNullable[DateOfBirth] and
    (__ \ "nino").formatNullable[String](ninoValidator) and
    (__ \ "role").formatNullable[String](roleValidator) and
    (__ \ "name").formatNullable[Name] and
    (__ \ "changeOfName").formatNullable[ChangeOfName] and
    (__ \ "currentOrPreviousAddress").formatNullable[CurrentOrPreviousAddress] and
    (__ \ "contact").formatNullable[OfficerContactDetails] and
    (__ \ "ivPassed").format[Boolean]
  )(VatLodgingOfficer.apply, unlift(VatLodgingOfficer.unapply))

}

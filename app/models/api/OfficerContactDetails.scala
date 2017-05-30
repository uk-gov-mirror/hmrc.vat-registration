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

case class OfficerContactDetails(email: Option[String] = None,
                                 tel: Option[String] = None,
                                 mobile: Option[String] = None)

object OfficerContactDetails extends VatLodgingOfficerValidator {

  implicit val format: Format[OfficerContactDetails] =
    ((__ \ "email").formatNullable[String](emailValidator) and
      (__ \ "tel").formatNullable[String](teleValidator) and
      (__ \ "mobile").formatNullable[String](mobileValidator)
      ) (OfficerContactDetails.apply, unlift(OfficerContactDetails.unapply))

  // $COVERAGE-OFF$

  val writesDES: Writes[OfficerContactDetails] = new Writes[OfficerContactDetails] {
    override def writes(officerContactDetails: OfficerContactDetails): JsValue = {
      val successWrites = (
        (__ \ "email").writeNullable[String] and
          (__ \ "tel").writeNullable[String] and
          (__ \ "mobile").writeNullable[String]
        ) (unlift(OfficerContactDetails.unapply))

      Json.toJson(officerContactDetails)(successWrites).as[JsObject]
    }
  }

  // $COVERAGE-ON$

}

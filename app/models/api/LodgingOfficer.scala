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

import deprecated.DeprecatedConstants
import play.api.libs.functional.syntax._
import play.api.libs.json._

case class LodgingOfficerDetails(currentAddress: Address,
                                 changeOfName: Option[FormerName],
                                 previousAddress : Option[Address],
                                 contact: DigitalContactOptional)

object LodgingOfficerDetails {
  implicit val format = (
    (__ \ "currentAddress").format[Address] and
    (__ \ "changeOfName").formatNullable[FormerName] and
    (__ \ "previousAddress").formatNullable[Address] and
    (__ \ "contact").format[DigitalContactOptional]
  )(LodgingOfficerDetails.apply, unlift(LodgingOfficerDetails.unapply))
}

case class LodgingOfficer(nino: String,
                          role: String,
                          name: Name,
                          details: Option[LodgingOfficerDetails],
                          isOfficerApplying: Boolean = true)

object LodgingOfficer extends VatLodgingOfficerValidator{

  val reads: Reads[LodgingOfficer] = (
    (__ \ "nino").read[String](ninoValidator) and
    (__ \ "role").read[String](roleValidator) and
    (__ \ "name").read[Name] and
    (__ \ "details").readNullable[LodgingOfficerDetails] and
    ((__ \ "isOfficerApplying").read[Boolean] or Reads.pure(true))
  )(LodgingOfficer.apply _)

  implicit val format: OFormat[LodgingOfficer] = OFormat(reads, Json.writes[LodgingOfficer])

  val patchJsonReads: Reads[JsObject] = {
    def apply(details: Option[LodgingOfficerDetails]): JsObject =
      details.fold(Json.obj())(d => Json.obj("details" -> d))
    (__ \ "details").readNullable[LodgingOfficerDetails].map(apply _)
  }

  val eligibilityDataJsonReads: Reads[(String, Name, String, Boolean)] = new Reads[(String, Name, String, Boolean)] {
    def nameRoleReads: Reads[(Name, String)] = (
      (__ \ "name").read[Name](Name.nameReadsFromElData) and
      (__ \ "role").read[String](roleValidator)
    )(Tuple2[Name, String] _)

    override def reads(json: JsValue): JsResult[(String, Name, String, Boolean)] = {
      JsSuccess((DeprecatedConstants.fakeNino, DeprecatedConstants.fakeOfficerName, "director", true))
    }
  }

  val mongoReads: Reads[LodgingOfficer] = new Reads[LodgingOfficer] {
    override def reads(json: JsValue): JsResult[LodgingOfficer] = {
      val officerDetails = (json \ "lodgingOfficer" \ "details").validateOpt[LodgingOfficerDetails].get

      json.validate[(String, Name, String, Boolean)](eligibilityDataJsonReads) map { tuple =>
        val (niNumber: String, officerName: Name, officerRole: String, isOfficerApplying: Boolean) = tuple
        LodgingOfficer(
          nino = niNumber,
          role = officerRole,
          name = officerName,
          isOfficerApplying = isOfficerApplying,
          details = officerDetails
        )
      }
    }
  }
}

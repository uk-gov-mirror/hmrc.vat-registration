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

case class ApplicantDetailsDetails(currentAddress: Address,
                                 changeOfName: Option[FormerName],
                                 previousAddress : Option[Address],
                                 contact: DigitalContactOptional)

object ApplicantDetailsDetails {
  implicit val format = (
    (__ \ "currentAddress").format[Address] and
    (__ \ "changeOfName").formatNullable[FormerName] and
    (__ \ "previousAddress").formatNullable[Address] and
    (__ \ "contact").format[DigitalContactOptional]
  )(ApplicantDetailsDetails.apply, unlift(ApplicantDetailsDetails.unapply))
}

case class ApplicantDetails(nino: String,
                            role: String,
                            name: Name,
                            details: Option[ApplicantDetailsDetails],
                            isApplicantApplying: Boolean = true)

object ApplicantDetails extends VatApplicantDetailsValidator{

  val reads: Reads[ApplicantDetails] = (
    (__ \ "nino").read[String](ninoValidator) and
    (__ \ "role").read[String](roleValidator) and
    (__ \ "name").read[Name] and
    (__ \ "details").readNullable[ApplicantDetailsDetails] and
    ((__ \ "isApplicantApplying").read[Boolean] or Reads.pure(true))
  )(ApplicantDetails.apply _)

  implicit val format: OFormat[ApplicantDetails] = OFormat(reads, Json.writes[ApplicantDetails])

  val patchJsonReads: Reads[JsObject] = {
    def apply(details: Option[ApplicantDetailsDetails]): JsObject =
      details.fold(Json.obj())(d => Json.obj("details" -> d))
    (__ \ "details").readNullable[ApplicantDetailsDetails].map(apply _)
  }

  val eligibilityDataJsonReads: Reads[(String, Name, String, Boolean)] = new Reads[(String, Name, String, Boolean)] {
    def nameRoleReads: Reads[(Name, String)] = (
      (__ \ "name").read[Name](Name.nameReadsFromElData) and
      (__ \ "role").read[String](roleValidator)
    )(Tuple2[Name, String] _)

    override def reads(json: JsValue): JsResult[(String, Name, String, Boolean)] = {
      JsSuccess((DeprecatedConstants.fakeNino, DeprecatedConstants.fakeApplicantName, "director", true))
    }
  }

  val mongoReads: Reads[ApplicantDetails] = new Reads[ApplicantDetails] {
    override def reads(json: JsValue): JsResult[ApplicantDetails] = {
      val officerDetails = (json \ "applicantDetails" \ "details").validateOpt[ApplicantDetailsDetails].get

      json.validate[(String, Name, String, Boolean)](eligibilityDataJsonReads) map { tuple =>
        val (niNumber: String, officerName: Name, officerRole: String, isApplicantApplying: Boolean) = tuple
        ApplicantDetails(
          nino = niNumber,
          role = officerRole,
          name = officerName,
          isApplicantApplying = isApplicantApplying,
          details = officerDetails
        )
      }
    }
  }
}

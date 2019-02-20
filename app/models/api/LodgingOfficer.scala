/*
 * Copyright 2019 HM Revenue & Customs
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

import play.api.libs.functional.syntax._
import play.api.libs.json._

import scala.collection.Seq

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

case class LodgingOfficer(dob: Option[LocalDate],
                          nino: String,
                          role: String,
                          name: Name,
                          ivPassed: Option[Boolean],
                          details: Option[LodgingOfficerDetails],
                          isOfficerApplying: Boolean = true)

object LodgingOfficer extends VatLodgingOfficerValidator{

  val reads: Reads[LodgingOfficer] = (
    (__ \ "dob").readNullable[LocalDate] and
    (__ \ "nino").read[String](ninoValidator) and
    (__ \ "role").read[String](roleValidator) and
    (__ \ "name").read[Name] and
    (__ \ "ivPassed").readNullable[Boolean] and
    (__ \ "details").readNullable[LodgingOfficerDetails] and
    ((__ \ "isOfficerApplying").read[Boolean] or Reads.pure(true))
  )(LodgingOfficer.apply _)

  implicit val format: OFormat[LodgingOfficer] = OFormat(reads, Json.writes[LodgingOfficer])

  val patchJsonReads: Reads[JsObject] = {
    def apply(dob: LocalDate, ivPassed: Option[Boolean], details: Option[LodgingOfficerDetails]): JsObject =
      Json.obj("dob" -> dob) ++ ivPassed.fold(Json.obj())(b => Json.obj("ivPassed" -> b)) ++ details.fold(Json.obj())(d => Json.obj("details" -> d))

    ((__ \ "dob").read[LocalDate] and
     (__ \ "ivPassed").readNullable[Boolean] and
     (__ \ "details").readNullable[LodgingOfficerDetails]
    )(apply _)
  }

  val eligibilityDataJsonReads: Reads[(String, Name, String, Boolean)] = new Reads[(String, Name, String, Boolean)] {
    def nameRoleReads: Reads[(Name, String)] = (
      (__ \ "name").read[Name](Name.nameReadsFromElData) and
      (__ \ "role").read[String](roleValidator)
    )(Tuple2[Name, String] _)

    override def reads(json: JsValue): JsResult[(String, Name, String, Boolean)] = {
      val niNumber = (json \ "applicantUKNino-optionalData").validate[String](ninoValidator)
      val completionCapacity = (json \ "completionCapacity").validate[JsValue]
      val ccFillingInFor = (json \ "completionCapacityFillingInFor").validateOpt[JsObject]

      val seqErrors = niNumber.fold(identity, _ => Seq.empty) ++
        completionCapacity.fold(identity, _ => Seq.empty) ++
        ccFillingInFor.fold(identity, _ => Seq.empty)

      if(seqErrors.nonEmpty) {
        JsError(seqErrors)
      } else {
        completionCapacity.get match {
          case s: JsString if s.value.toLowerCase == "noneofthese" =>
            ccFillingInFor.get.fold[JsResult[(String, Name, String, Boolean)]] {
              JsError("completionCapacity is 'NoneOfThese' but missing JsObject for completionCapacityFillingInFor")
            } {_.validate[(Name, String)](nameRoleReads) map { tuple =>
                val (name, role) = tuple
                (niNumber.get, name, role, false)
              }
            }
          case cc: JsObject =>
            cc.validate[(Name, String)](nameRoleReads) map { tuple =>
              val (name, role) = tuple
              (niNumber.get, name, role, true)
            }
          case _ =>
            JsError("completionCapacity does not contain a JsObject or correct JsString of 'NoneOfThese'")
        }
      }
    }
  }

  val mongoReads: Reads[LodgingOfficer] = new Reads[LodgingOfficer] {
    override def reads(json: JsValue): JsResult[LodgingOfficer] = {
      val dateOfBirth = (json \ "lodgingOfficer" \ "dob").validateOpt[LocalDate].get
      val ivDone = (json \ "lodgingOfficer" \ "ivPassed").validateOpt[Boolean].get
      val officerDetails = (json \ "lodgingOfficer" \ "details").validateOpt[LodgingOfficerDetails].get

      json.validate[(String, Name, String, Boolean)](eligibilityDataJsonReads) map { tuple =>
        val (niNumber: String, officerName: Name, officerRole: String, isOfficerApplying: Boolean) = tuple
        LodgingOfficer(
          dob = dateOfBirth,
          nino = niNumber,
          role = officerRole,
          name = officerName,
          ivPassed = ivDone,
          isOfficerApplying = isOfficerApplying,
          details = officerDetails
        )
      }
    }
  }
}

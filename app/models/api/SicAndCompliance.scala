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

import models.api.Returns.JsonUtilities
import play.api.libs.functional.syntax._
import play.api.libs.json._

case class SicAndCompliance(businessDescription: String,
                            labourCompliance: Option[ComplianceLabour],
                            mainBusinessActivity: SicCode,
                            businessActivities: List[SicCode]) {

  def otherBusinessActivities: List[SicCode] =
    businessActivities.filterNot(_ == mainBusinessActivity)
}

object SicAndCompliance {

  val mongoReads: Reads[SicAndCompliance] = {
    implicit val sicCodeMongoFormat: Format[SicCode] = SicCode.mongoFormat
    ((__ \ "businessDescription").read[String] and
      (__ \ "labourCompliance").readNullable[ComplianceLabour] and
      (__ \ "mainBusinessActivity").read[SicCode] and
      (__ \ "businessActivities").read[List[SicCode]]
      ) (SicAndCompliance.apply _)
  }

  val apiReads: Reads[SicAndCompliance] = {
    implicit val sicCodeApiFormat: Format[SicCode] = SicCode.apiFormat
    ((__ \ "businessDescription").read[String] and
      (__ \ "labourCompliance").readNullable[ComplianceLabour] and
      (__ \ "mainBusinessActivity").read[SicCode] and
      (__ \ "businessActivities").read[List[SicCode]]
      ) (SicAndCompliance.apply _)
  }

  val writes: Writes[SicAndCompliance] = {
    implicit val sicCodeApiFormat: Format[SicCode] = SicCode.mongoFormat
    ((__ \ "businessDescription").write[String] and
      (__ \ "labourCompliance").writeNullable[ComplianceLabour] and
      (__ \ "mainBusinessActivity").write[SicCode] and
      (__ \ "businessActivities").write[List[SicCode]]
      ) (unlift(SicAndCompliance.unapply))
  }

  implicit val apiFormats: Format[SicAndCompliance] = Format(apiReads, writes)
  val mongoFormats: Format[SicAndCompliance] = Format(mongoReads, writes)

  val submissionReads: Reads[SicAndCompliance] = (
    (__ \ "subscription" \ "businessActivities" \ "description").read[String] and
      (__ \ "compliance").readNullable[ComplianceLabour](ComplianceLabour.submissionFormat) and
      (__ \ "subscription" \ "businessActivities" \ "SICCodes" \ "primaryMainCode").read[String].fmap(code => SicCode(code, "", "")) and
      (__ \ "subscription" \ "businessActivities" \ "SICCodes").read[List[SicCode]](SicCode.sicCodeListReads).orElse(Reads.pure(List()))
    ) (apply(_, _, _, _))

  val submissionWrites: Writes[SicAndCompliance] = Writes { sicAndCompliance: SicAndCompliance =>
    Json.obj(
      "subscription" -> Json.obj(
        "businessActivities" -> Json.obj(
          "description" -> sicAndCompliance.businessDescription,
          "SICCodes" -> (
            Json.obj(
              "primaryMainCode" -> sicAndCompliance.mainBusinessActivity.id
            ) ++ Json.toJson(sicAndCompliance.otherBusinessActivities)(SicCode.sicCodeListWrites).as[JsObject]
          )
        )
      ),
      "compliance" -> sicAndCompliance.labourCompliance.map(Json.toJson(_)(ComplianceLabour.submissionFormat))
    ).filterNullFields
  }

}

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

import play.api.libs.json._
import play.api.libs.functional.syntax._

case class SicAndCompliance(businessDescription: String,
                            labourCompliance: Option[ComplianceLabour],
                            mainBusinessActivity: SicCode,
                            otherBusinessActivities: List[SicCode])

object SicAndCompliance {

  val mongoReads: Reads[SicAndCompliance] = {
    implicit val sicCodeMongoFormat = SicCode.mongoFormat
    ( (__ \ "businessDescription").read[String] and
      (__ \ "labourCompliance").readNullable[ComplianceLabour] and
      (__ \ "mainBusinessActivity").read[SicCode] and
      (__ \ "otherBusinessActivities").read[List[SicCode]]
      )(SicAndCompliance.apply _)
  }

  val apiReads: Reads[SicAndCompliance] = {
    implicit val sicCodeApiFormat = SicCode.apiFormat
    ( (__ \ "businessDescription").read[String] and
      (__ \ "labourCompliance").readNullable[ComplianceLabour] and
      (__ \ "mainBusinessActivity").read[SicCode] and
      (__ \ "otherBusinessActivities").read[List[SicCode]]
      )(SicAndCompliance.apply _)
  }

  val writes: Writes[SicAndCompliance] = {
    implicit val sicCodeApiFormat = SicCode.mongoFormat
    ( (__ \ "businessDescription").write[String] and
      (__ \ "labourCompliance").writeNullable[ComplianceLabour] and
      (__ \ "mainBusinessActivity").write[SicCode] and
      (__ \ "otherBusinessActivities").write[List[SicCode]]
      )(unlift(SicAndCompliance.unapply))
  }

  implicit val apiFormats: Format[SicAndCompliance] = Format(apiReads,writes)
  val mongoFormats: Format[SicAndCompliance] = Format(mongoReads,writes)

  val submissionReads: Reads[SicAndCompliance] = (
    (__ \ "description").read[String] and
    Reads.pure(None) and
    (__ \ "SICCodes" \ "primaryMainCode").read[String].fmap(code => SicCode(code, "", "")) and
    (__ \ "SICCodes").read[List[SicCode]](SicCode.sicCodeListReads)
  )(apply(_, _, _, _))

  val submissionWrites: Writes[SicAndCompliance] = (
    (__ \ "description").write[String] and
    (__).writeNullable[ComplianceLabour].contramap[Option[ComplianceLabour]](_ => None) and
    (__ \ "SICCodes" \ "primaryMainCode").write[String].contramap[SicCode](code => code.id) and
    (__ \ "SICCodes").write[List[SicCode]](SicCode.sicCodeListWrites)
  )(unlift(unapply))

  val submissionFormat: Format[SicAndCompliance] = Format[SicAndCompliance](submissionReads, submissionWrites)

}

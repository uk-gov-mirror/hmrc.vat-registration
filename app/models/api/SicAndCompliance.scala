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
  }


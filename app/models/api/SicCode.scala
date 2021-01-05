/*
 * Copyright 2021 HM Revenue & Customs
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

case class SicCode(id: String,
                description: String,
                displayDetails: String)

object SicCode extends SicCodeValidator {

  val writes: Writes[SicCode] = (
    (__ \ "code").write[String] and
    (__ \ "desc").write[String] and
    (__ \ "indexes").write[String]
  )(unlift(SicCode.unapply))

  val mongoReads: Reads[SicCode] = (
      (__ \ "code").read[String] and
      (__ \ "desc").read[String] and
      (__ \ "indexes").read[String]
    )(SicCode.apply _)

  val apiReads: Reads[SicCode] = (
      (__ \ "code").read[String](idValidator) and
      (__ \ "desc").read[String] and
      (__ \ "indexes").read[String]
    )(SicCode.apply _)

  val apiFormat = Format(apiReads,writes)
  val mongoFormat = Format(mongoReads,writes)

  val sicCodeListReads = Reads[List[SicCode]] { json =>
    (
      (__ \ "mainCode2").readNullable[String] and
      (__ \ "mainCode3").readNullable[String] and
      (__ \ "mainCode4").readNullable[String]
    ) ((code1, code2, code3) => (code1 ++ code2 ++ code3).toList.map(c => SicCode(c, "", ""))).reads(json)
  }

  val sicCodeListWrites = Writes[List[SicCode]] { codes =>
    Json.obj(
      "mainCode2" -> codes.lift(0).map(_.id),
      "mainCode3" -> codes.lift(1).map(_.id),
      "mainCode4" -> codes.lift(2).map(_.id)
    ) match {
      case JsObject(fieldSet) => JsObject(fieldSet.flatMap {
        case (_, JsNull) => None
        case _@value => Some(value)
      })
      case obj => obj
    }
  }

}

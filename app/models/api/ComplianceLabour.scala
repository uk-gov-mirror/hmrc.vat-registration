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

import play.api.libs.functional.syntax._
import play.api.libs.json.{Format, OFormat, __}

case class ComplianceLabour(numberOfWorkers: Int,
                            temporaryContracts: Option[Boolean],
                            skilledWorkers: Option[Boolean])

object ComplianceLabour {

  implicit val formats: OFormat[ComplianceLabour] = (
    (__ \"numberOfWorkers").format[Int] and
    (__ \"temporaryContracts").formatNullable[Boolean] and
    (__ \"skilledWorkers").formatNullable[Boolean]
  )(apply, unlift(unapply))

  val submissionFormat: Format[ComplianceLabour] = (
    (__ \ "numOfWorkers").format[Int] and
    (__ \ "tempWorkers").formatNullable[Boolean] and
    (__ \ "provisionOfLabour").formatNullable[Boolean]
  )(apply, unlift(unapply))

}

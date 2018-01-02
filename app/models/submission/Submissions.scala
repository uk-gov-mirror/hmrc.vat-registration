/*
 * Copyright 2018 HM Revenue & Customs
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

package models.submission

import java.time.LocalDate

import org.joda.time.DateTime
import play.api.libs.functional.syntax._
import play.api.libs.functional.syntax.unlift
import play.api.libs.json.{Writes, __}

case class DESSubmission(acknowledgementReference: String,
                         companyName: String,
                         vatStartDate: Option[LocalDate] = None,
                         incorpDate: Option[LocalDate] = None)

object DESSubmission {
  implicit val writes: Writes[DESSubmission] =
    (
      (__ \ "acknowledgementReference").write[String] and
        (__ \ "companyName").write[String] and
        (__ \ "vatStartDate").writeNullable[LocalDate] and
        (__ \ "incorpDate").writeNullable[LocalDate]
      )(unlift(DESSubmission.unapply))
}

case class TopUpSubmission(acknowledgementReference: String,
                         status: String,
                         vatStartDate: Option[LocalDate] = None,
                         incorpDate: Option[DateTime] = None)

object TopUpSubmission {
  implicit val writes: Writes[TopUpSubmission] =
    (
      (__ \ "acknowledgementReference").write[String] and
        (__ \ "status").write[String] and
        (__ \ "vatStartDate").writeNullable[LocalDate] and
        (__ \ "incorpDate").writeNullable[DateTime]
      )(unlift(TopUpSubmission.unapply))
}

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

package models.api

import models.api.VatChoice.{necessityValidator, reasonValidator}
import play.api.libs.functional.syntax._
import play.api.libs.json._

@deprecated("Use Threshold instead", "12/12/2017")
case class VatEligibilityChoice(necessity: String, // "obligatory" or "voluntary"
                                reason: Option[String] = None,
                                vatThresholdPostIncorp: Option[VatThresholdPostIncorp] = None,
                                vatExpectedThresholdPostIncorp: Option[VatExpectedThresholdPostIncorp] = None)

@deprecated("Use Threshold instead", "12/12/2017")
object VatEligibilityChoice {
  implicit val format: OFormat[VatEligibilityChoice] = (
    (__ \ "necessity").format[String](necessityValidator) and
    (__ \ "reason").formatNullable[String](reasonValidator) and
    (__ \ "vatThresholdPostIncorp").formatNullable[VatThresholdPostIncorp] and
    (__ \ "vatExpectedThresholdPostIncorp").formatNullable[VatExpectedThresholdPostIncorp]
  )(VatEligibilityChoice.apply, unlift(VatEligibilityChoice.unapply))
}

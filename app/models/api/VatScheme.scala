/*
 * Copyright 2017 HM Revenue & Customs
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

import common.RegistrationId
import play.api.libs.functional.syntax._
import play.api.libs.json._

case class VatScheme(
                      id: RegistrationId,
                      tradingDetails: Option[VatTradingDetails] = None,
                      financials: Option[VatFinancials] = None,
                      vatSicAndCompliance: Option[VatSicAndCompliance] = None,
                      vatContact: Option[VatContact] = None,
                      vatEligibility: Option[VatServiceEligibility] = None
                    )

object VatScheme {

  def reads(implicit r: Reads[VatFinancials]): Reads[VatScheme] = (
    (__ \ "registrationId").read[RegistrationId] and
      (__ \ "tradingDetails").readNullable[VatTradingDetails] and
      (__ \ "financials").readNullable[VatFinancials](r) and
      (__ \ "vatSicAndCompliance").readNullable[VatSicAndCompliance] and
      (__ \ "vatContact").readNullable[VatContact] and
      (__ \ "vatEligibility").readNullable[VatServiceEligibility]
    ) (VatScheme.apply _)


  def writes(implicit w: Writes[VatFinancials]): OWrites[VatScheme] = (
    (__ \ "registrationId").write[RegistrationId] and
      (__ \ "tradingDetails").writeNullable[VatTradingDetails] and
      (__ \ "financials").writeNullable[VatFinancials](w) and
      (__ \ "vatSicAndCompliance").writeNullable[VatSicAndCompliance] and
      (__ \ "vatContact").writeNullable[VatContact] and
      (__ \ "vatEligibility").writeNullable[VatServiceEligibility]
    ) (unlift(VatScheme.unapply))

  implicit def format(implicit f: OFormat[VatFinancials]): OFormat[VatScheme] = OFormat(reads(f), writes(f))

}

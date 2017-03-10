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

package models

import play.api.libs.functional.syntax._
import play.api.libs.json._

case class VatScheme(
                      id: String,
                      tradingDetails: Option[VatTradingDetails] = None,
                      vatChoice: Option[VatChoice] = None, //why is this not in schema https://github.tools.tax.service.gov.uk/HMRC/business-registration-interface/blob/master/des/vat/schema/json-schema/btr.vat.scheme.schema.json
                      financials: Option[VatFinancials] = None, //why is all option?
                      sicAndCompliance: Option[VatSicAndCompliance] = None
                    )

object VatScheme {

  implicit val financialsFormat = VatFinancials.format

  def cTReads(rdsAck: Reads[VatFinancials]): Reads[VatScheme] = (
    (__ \ "ID").read[String] and
      (__ \ "trading-details").readNullable[VatTradingDetails] and
      (__ \ "vat-choice").readNullable[VatChoice] and
      (__ \ "financials").readNullable[VatFinancials](rdsAck) and
      (__ \ "sic-and-compliance").readNullable[VatSicAndCompliance]
    ) (VatScheme.apply _)


  def cTWrites(wtsAck: Writes[VatFinancials]): OWrites[VatScheme] = (
    (__ \ "ID").write[String] and
      (__ \ "trading-details").writeNullable[VatTradingDetails] and
      (__ \ "vat-choice").writeNullable[VatChoice] and
      (__ \ "financials").writeNullable[VatFinancials](wtsAck) and
      (__ \ "sic-and-compliance").writeNullable[VatSicAndCompliance]
    ) (unlift(VatScheme.unapply))

  implicit val format: OFormat[VatScheme] = OFormat(cTReads(financialsFormat), cTWrites(financialsFormat))


}
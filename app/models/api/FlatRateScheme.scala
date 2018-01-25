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

import play.api.data.validation.ValidationError
import play.api.libs.json._

case class FlatRateScheme(joinFrs: Boolean,
                          frsDetails: Option[FRSDetails])

object FlatRateScheme {
  implicit val read: Reads[FlatRateScheme] = Json.reads[FlatRateScheme]
    .filter(ValidationError(Seq("Mismatch between frsDetails presence and joinFrs"))) { frs =>
      frs.frsDetails match {
      case Some(_) => frs.joinFrs
      case None => !frs.joinFrs
    }
  }

  implicit val writes: OWrites[FlatRateScheme] = Json.writes[FlatRateScheme]
}

case class FRSDetails(overBusinessGoods: Boolean,
                      overBusinessGoodsPercent: Option[Boolean],
                      vatInclusiveTurnover: Option[Long],
                      start: Option[StartDate],
                      categoryOfBusiness: String,
                      percent: BigDecimal)

object FRSDetails {
  implicit val reads: Reads[FRSDetails] = Json.reads[FRSDetails]
    .filter(ValidationError(Seq("Mismatch between vatInclusiveTurnover presence and overBusinessGoods"))) { details =>
      details.vatInclusiveTurnover match {
        case Some(_) => !details.overBusinessGoods
        case None => details.overBusinessGoods
      }
    }

  implicit val writes: Writes[FRSDetails] = Json.writes[FRSDetails]
}

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

import java.time.LocalDate

import play.api.libs.json._
import play.api.libs.functional.syntax._

case class FlatRateScheme(joinFrs: Boolean,
                          frsDetails: Option[FRSDetails])

object FlatRateScheme {
  implicit val format: Format[FlatRateScheme] = Json.format[FlatRateScheme]
}

case class BusinessGoods(estimatedTotalSales: Long, overTurnover: Boolean)

object BusinessGoods {
  implicit val format: OFormat[BusinessGoods] = Json.format[BusinessGoods]
}

case class FRSDetails(businessGoods: Option[BusinessGoods],
                      startDate: Option[LocalDate],
                      categoryOfBusiness: String,
                      percent: BigDecimal)

object FRSDetails {
  implicit val format: Format[FRSDetails] = Json.format[FRSDetails]

  val submissionReads: Reads[FRSDetails] = (
    (__ \ "startDate").readNullable[LocalDate] and
    (__ \ "FRSCategory").read[String] and
    (__ \ "FRSPercentage").read[BigDecimal]
  )(FRSDetails.apply(None, _, _, _))

  val submissionWrites: Writes[FRSDetails] = Writes[FRSDetails] { frs =>
    Json.obj(
      "startDate" -> frs.startDate,
      "FRSCategory" -> frs.categoryOfBusiness,
      "FRSPercentage" -> frs.percent
    )
  }

  val submissionFormat: Format[FRSDetails] = Format[FRSDetails](submissionReads, submissionWrites)

  val auditWrites: Writes[FRSDetails] = Writes[FRSDetails] { frs =>
    Json.obj(
      "startDate" -> frs.startDate,
      "frsCategory" -> frs.categoryOfBusiness,
      "frsPercentage" -> frs.percent
    )
  }
}
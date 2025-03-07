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

package services.monitoring

import models.api.Returns.writePeriod
import models.api.VatScheme
import play.api.libs.json._
import uk.gov.hmrc.http.InternalServerException
import utils.JsonUtils.jsonObject

import javax.inject.Singleton

@Singleton
class PeriodsAuditBlockBuilder {

  def buildPeriodsBlock(vatScheme: VatScheme): JsObject = {
    vatScheme.returns match {
      case Some(returns) =>
        writePeriod(returns.frequency, returns.staggerStart)
          .map(period => jsonObject(
            "customerPreferredPeriodicity" -> period
          ))
          .getOrElse(
            throw new InternalServerException("[PeriodsBlockBuilder]: Couldn't build periods section due to either an invalid frequency or stagger start")
          )
      case None =>
        throw new InternalServerException("[PeriodsBlockBuilder]: Couldn't build periods section due to missing returns section in vat scheme")
    }
  }
}

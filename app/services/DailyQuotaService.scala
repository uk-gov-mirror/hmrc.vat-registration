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

package services

import config.BackendConfig
import javax.inject.Inject
import repositories.trafficmanagement.DailyQuotaRepository
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

class DailyQuotaService @Inject()(dailyQuotaRepository: DailyQuotaRepository)
                                 (implicit ec: ExecutionContext, config: BackendConfig) {

  def canAllocate(implicit hc: HeaderCarrier): Future[Boolean] =
    dailyQuotaRepository.getCurrentTotal.map(_ < config.dailyQuota)

  def incrementTotal(implicit hc: HeaderCarrier): Future[Int] =
    dailyQuotaRepository.updateCurrentTotal

}

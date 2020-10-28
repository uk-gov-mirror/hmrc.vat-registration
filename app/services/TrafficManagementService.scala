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

import javax.inject.{Inject, Singleton}
import models.api.{Draft, OTRS, RegistrationChannel, RegistrationInformation, RegistrationStatus, VatReg}
import repositories.trafficmanagement.{DailyQuotaRepository, TrafficManagementRepository}
import uk.gov.hmrc.http.HeaderCarrier
import utils.TimeMachine

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class TrafficManagementService @Inject()(dailyQuotaRepository: DailyQuotaRepository,
                                         val trafficManagementRepository: TrafficManagementRepository,
                                         timeMachine: TimeMachine)
                                        (implicit ec: ExecutionContext) {

  def allocate(internalId: String, regId: String)(implicit hc: HeaderCarrier): Future[AllocationResponse] = {
    for {
      quotaReached <- dailyQuotaRepository.checkQuota
      channel = if (quotaReached) OTRS else VatReg
      _ <- trafficManagementRepository.upsertRegistrationInformation(internalId, regId, Draft, Some(timeMachine.today), channel)
    } yield if (quotaReached) QuotaReached else Allocated
  }

  def getRegistrationInformation(internalId: String)(implicit hc: HeaderCarrier): Future[Option[RegistrationInformation]] =
    trafficManagementRepository.getRegistrationInformation(internalId)

}

sealed trait AllocationResponse

case object QuotaReached extends AllocationResponse

case object Allocated extends AllocationResponse
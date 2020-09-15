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
import models.api.{RegistrationChannel, RegistrationInformation, RegistrationStatus}
import repositories.trafficmanagement.{DailyQuotaRepository, TrafficManagementRepository}
import uk.gov.hmrc.http.HeaderCarrier
import utils.TimeMachine

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class TrafficManagementService @Inject()(dailyQuotaRepository: DailyQuotaRepository,
                                         val trafficManagementRepository: TrafficManagementRepository,
                                         timeMachine: TimeMachine)
                                        (implicit ec: ExecutionContext) {

  def getRegistrationInformation(internalId: String)(implicit hc: HeaderCarrier): Future[Option[RegistrationInformation]] =
    trafficManagementRepository.getRegistrationInformation(internalId)

  def upsertRegistrationInformation(internalId: String, regId: String, status: RegistrationStatus, channel: RegistrationChannel)
                                   (implicit hc: HeaderCarrier): Future[RegistrationInformation] = {
    getRegistrationInformation(internalId) flatMap {
      case None =>
        for {
          regInfo <- trafficManagementRepository.upsertRegistrationInformation(internalId, regId, status, Some(timeMachine.today), channel)
          _ <- dailyQuotaRepository.incrementTotal
        } yield regInfo
      case Some(existingRecord) =>
        trafficManagementRepository.upsertRegistrationInformation(internalId, regId, status, existingRecord.regStartDate, channel)
    }
  }

}

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

import java.util.UUID

import config.BackendConfig
import featureswitch.core.config.{FeatureSwitching, TrafficManagement}
import javax.inject.{Inject, Singleton}
import models.api.{Draft, OTRS, VatScheme}
import repositories.RegistrationMongoRepository
import repositories.trafficmanagement.DailyQuotaRepository
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class NewRegistrationService @Inject()(dailyQuotaRepository: DailyQuotaRepository,
                                       registrationRepository: RegistrationMongoRepository,
                                       trafficManagementService: TrafficManagementService)
                                      (implicit ec: ExecutionContext, config: BackendConfig) extends FeatureSwitching {

  def newRegistration(internalId: String)(implicit hc: HeaderCarrier): Future[RegistrationResponse] = {
    val regId = generateRegistrationId()

    if (isEnabled(TrafficManagement)) {
      dailyQuotaRepository.quotaReached flatMap { quotaReached =>
        if (quotaReached) {
          trafficManagementService.upsertRegistrationInformation(internalId, regId, Draft, OTRS) map (_ => QuotaReached)
        }
        else {
          registrationRepository.createNewVatScheme(regId, internalId) map { vatScheme =>
            RegistrationCreated(vatScheme)
          }
        }
      }
    }
    else {
      registrationRepository.createNewVatScheme(regId, internalId) map { vatScheme =>
        RegistrationCreated(vatScheme)
      }
    }
  }

  private[services] def generateRegistrationId(): String = UUID.randomUUID().toString

}

sealed trait RegistrationResponse

case object QuotaReached extends RegistrationResponse

case class RegistrationCreated(vatScheme: VatScheme) extends RegistrationResponse
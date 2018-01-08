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

package services

import javax.inject.Inject
import models.api.SicAndCompliance
import repositories.{RegistrationMongo, RegistrationMongoRepository}
import scala.concurrent.{ExecutionContext, Future}

class SicAndComplianceService @Inject()(registrationMongo: RegistrationMongo) extends SicAndComplianceSrv {
  val registrationRepository: RegistrationMongoRepository = registrationMongo.store
}

trait SicAndComplianceSrv{
  val registrationRepository: RegistrationMongoRepository

  def getSicAndCompliance(regId: String)(implicit ec: ExecutionContext): Future[Option[SicAndCompliance]] =
    registrationRepository.getSicAndCompliance(regId)

  def updateSicAndCompliance(regId: String, sicAndCompliance: SicAndCompliance)(implicit ec: ExecutionContext): Future[SicAndCompliance] =
    registrationRepository.updateSicAndCompliance(regId,sicAndCompliance)
}

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

import javax.inject.Inject
import models.api.{IncomingRegistrationInformation, RegistrationInformation}
import repositories.trafficmanagement.RegistrationInformationRepository
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

class RegistrationInformationService @Inject()(val regInfoRepo: RegistrationInformationRepository)
                                              (implicit ec: ExecutionContext) {

  def getRegistrationInformation(internalId: String)
                                (implicit hc: HeaderCarrier): Future[Option[RegistrationInformation]] =
    regInfoRepo.getRegistrationInformation(internalId)

  def upsertRegistrationInformation(internalId: String,
                                    regInfo: IncomingRegistrationInformation)
                                   (implicit hc: HeaderCarrier): Future[RegistrationInformation] =
    regInfoRepo.upsertRegistrationInformation(internalId, regInfo)

}

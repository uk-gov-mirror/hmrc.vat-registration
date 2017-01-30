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

package services

import javax.inject.Inject

import connectors.{BusinessRegistrationConnector, BusinessRegistrationSuccessResponse}
import models.VatScheme
import repositories.RegistrationRepository
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.Future

trait RegistrationService {

  def createNewRegistration(internalId: String)(implicit headerCarrier: HeaderCarrier): Future[Either[Exception, VatScheme]]

}

class VatRegistrationService @Inject()(brConnector: BusinessRegistrationConnector, registrationRepository: RegistrationRepository) extends RegistrationService {

  import scala.concurrent.ExecutionContext.Implicits.global

  override def createNewRegistration(internalId: String)(implicit headerCarrier: HeaderCarrier): Future[Either[Exception, VatScheme]] = {
    (for {
      BusinessRegistrationSuccessResponse(profile) <- brConnector.retrieveCurrentProfile
      registration <- registrationRepository.createNewRegistration(profile.registrationID, internalId)
    } yield Right(VatScheme(registration.registrationId))) recover {
      case ex: Exception => Left(ex)
    }
  }

}

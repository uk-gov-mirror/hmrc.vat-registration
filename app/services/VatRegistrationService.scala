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

import cats.data.EitherT
import common.exceptions._
import connectors._
import models._
import models.external.CurrentProfile
import repositories.RegistrationRepository
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait RegistrationService {

  def createNewRegistration(implicit headerCarrier: HeaderCarrier): ServiceResult[VatScheme]

  def updateVatChoice(registrationId: String, vatChoice: VatChoice): ServiceResult[VatChoice]

  def updateTradingDetails(registrationId: String, tradingDetails: VatTradingDetails): ServiceResult[VatTradingDetails]

}

class VatRegistrationService @Inject()(brConnector: BusinessRegistrationConnector,
                                       registrationRepository: RegistrationRepository
                                      ) extends RegistrationService {

  import cats.implicits._

  private def toEitherT[T](eventualT: Future[T]) =
    EitherT[Future, LeftState, T](eventualT.map(Right(_)).recover { case t => Left(GenericError(t)) })

  private def getOrCreateVatScheme(profile: CurrentProfile): Future[Either[LeftState, VatScheme]] =
    registrationRepository.retrieveVatScheme(profile.registrationID).flatMap {
      case Some(vatScheme) => Future.successful(Right(vatScheme))
      case None => registrationRepository.createNewVatScheme(profile.registrationID).map(Right(_)).recover {
        case t => Left(GenericError(t))
      }
    }

  override def createNewRegistration(implicit headerCarrier: HeaderCarrier): ServiceResult[VatScheme] =
    for {
      profile <- EitherT(brConnector.retrieveCurrentProfile)
      vatScheme <- EitherT(getOrCreateVatScheme(profile))
    } yield vatScheme

  override def updateVatChoice(registrationId: String, vatChoice: VatChoice): ServiceResult[VatChoice] =
    toEitherT(registrationRepository.updateVatChoice(registrationId, vatChoice))

  override def updateTradingDetails(registrationId: String, tradingDetails: VatTradingDetails): ServiceResult[VatTradingDetails] =
    toEitherT(registrationRepository.updateTradingDetails(registrationId, tradingDetails))

}

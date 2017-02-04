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
import common.exceptions.{GenericError, LeftState}
import connectors._
import models.{VatChoice, VatScheme, VatTradingDetails}
import repositories.RegistrationRepository
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.Future

trait RegistrationService {

  def createNewRegistration(implicit headerCarrier: HeaderCarrier): Future[ServiceResult[VatScheme]]

  def updateVatChoice(registrationId: String, vatChoice: VatChoice): Future[ServiceResult[VatChoice]]

  def updateTradingDetails(registrationId: String, tradingDetails: VatTradingDetails): Future[ServiceResult[VatTradingDetails]]

}

class VatRegistrationService @Inject()(brConnector: BusinessRegistrationConnector,
                                       registrationRepository: RegistrationRepository
                                      ) extends RegistrationService {

  import cats.implicits._

  import scala.concurrent.ExecutionContext.Implicits.global

  override def createNewRegistration(implicit headerCarrier: HeaderCarrier): Future[ServiceResult[VatScheme]] = {
    val futureVatScheme = for {
      profile <- EitherT(brConnector.retrieveCurrentProfile)
      vatScheme <- EitherT[Future, LeftState, VatScheme](
        registrationRepository.retrieveVatScheme(profile.registrationID).flatMap {
          case Some(vatScheme) => Future.successful(Right(vatScheme))
          case None => registrationRepository.createNewVatScheme(profile.registrationID).map(Right(_)).recover {
            case t => Left(GenericError(t))
          }
        })
    } yield vatScheme
    futureVatScheme.value
  }

  override def updateVatChoice(registrationId: String, vatChoice: VatChoice): Future[ServiceResult[VatChoice]] =
    registrationRepository.updateVatChoice(registrationId, vatChoice).map(Right(_))
      .recover(genericServiceException)

  override def updateTradingDetails(registrationId: String, tradingDetails: VatTradingDetails): Future[ServiceResult[VatTradingDetails]] =
    registrationRepository.updateTradingDetails(registrationId, tradingDetails).map(Right(_))
      .recover(genericServiceException)

}

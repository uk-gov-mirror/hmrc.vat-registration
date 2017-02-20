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

  def createNewRegistration()(implicit headerCarrier: HeaderCarrier): ServiceResult[VatScheme]

  def retrieveVatScheme(registrationId: String): ServiceResult[VatScheme]

  def updateVatChoice(registrationId: String, vatChoice: VatChoice): ServiceResult[VatChoice]

  def updateTradingDetails(registrationId: String, tradingDetails: VatTradingDetails): ServiceResult[VatTradingDetails]

  def updateVatFinancials(registrationId: String, financials: VatFinancials): ServiceResult[VatFinancials]

  def deleteVatScheme(registrationId: String): ServiceResult[Boolean]

  def dropCollection: Future[Unit]

}

class VatRegistrationService @Inject()(brConnector: BusinessRegistrationConnector,
                                       registrationRepository: RegistrationRepository
                                      ) extends RegistrationService {

  import cats.implicits._

  private def repositoryErrorHandler[T]: PartialFunction[Throwable, Either[LeftState, T]] = {
    case e: MissingRegDocument => Left(ResourceNotFound(s"No registration found for registration ID: ${e.regId}"))
    case dbe: DBExceptions => Left(GenericDatabaseError(dbe, Some(dbe.regId)))
    case t: Throwable => Left(GenericError(t))
  }

  private def toEitherT[T](eventualT: Future[T]) =
    EitherT[Future, LeftState, T](eventualT.map(Right(_)).recover(repositoryErrorHandler))

  private def getOrCreateVatScheme(profile: CurrentProfile): Future[Either[LeftState, VatScheme]] =
    registrationRepository.retrieveVatScheme(profile.registrationID).flatMap {
      case Some(vatScheme) => Future.successful(Right(vatScheme))
      case None => registrationRepository.createNewVatScheme(profile.registrationID).map(Right(_)).recover(repositoryErrorHandler)
    }

  override def createNewRegistration()(implicit headerCarrier: HeaderCarrier): ServiceResult[VatScheme] =
    for {
      profile <- EitherT(brConnector.retrieveCurrentProfile)
      vatScheme <- EitherT(getOrCreateVatScheme(profile))
    } yield vatScheme

  override def retrieveVatScheme(registrationId: String): ServiceResult[VatScheme] =
    EitherT(registrationRepository.retrieveVatScheme(registrationId).map[Either[LeftState, VatScheme]] {
      case Some(vatScheme) => Right(vatScheme)
      case None => Left(ResourceNotFound(registrationId))
    })

  override def updateVatChoice(registrationId: String, vatChoice: VatChoice): ServiceResult[VatChoice] =
    toEitherT(registrationRepository.updateVatChoice(registrationId, vatChoice))

  override def updateTradingDetails(registrationId: String, tradingDetails: VatTradingDetails): ServiceResult[VatTradingDetails] =
    toEitherT(registrationRepository.updateTradingDetails(registrationId, tradingDetails))

  override def updateVatFinancials(registrationId: String, financials: VatFinancials): ServiceResult[VatFinancials] =
    toEitherT(registrationRepository.updateVatFinancials(registrationId, financials))

  override def deleteVatScheme(registrationId: String): ServiceResult[Boolean] =
    toEitherT(registrationRepository.deleteVatScheme(registrationId))


  override def dropCollection: Future[Unit] = { registrationRepository.dropCollection}

}

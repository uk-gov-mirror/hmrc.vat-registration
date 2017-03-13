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
import common.Identifiers.RegistrationId
import common.LogicalGroup
import common.exceptions._
import connectors._
import models._
import models.external.CurrentProfile
import play.api.libs.json.Format
import repositories.RegistrationRepository
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait RegistrationService {

  def createNewRegistration()(implicit headerCarrier: HeaderCarrier): ServiceResult[VatScheme]

  def retrieveVatScheme(rid: RegistrationId): ServiceResult[VatScheme]

  def updateLogicalGroup[G: LogicalGroup : Format](rid: RegistrationId, group: G): ServiceResult[G]

  def deleteVatScheme(rid: RegistrationId): ServiceResult[Boolean]

  def deleteBankAccountDetails(rid: RegistrationId): ServiceResult[Boolean]

  def deleteZeroRatedTurnover(rid: RegistrationId): ServiceResult[Boolean]

  def deleteAccountingPeriodStart(rid: RegistrationId): ServiceResult[Boolean]

}

class VatRegistrationService @Inject()(brConnector: BusinessRegistrationConnector,
                                       registrationRepository: RegistrationRepository
                                      ) extends RegistrationService {

  import cats.implicits._

  private def repositoryErrorHandler[T]: PartialFunction[Throwable, Either[LeftState, T]] = {
    case e: MissingRegDocument => Left(ResourceNotFound(s"No registration found for registration ID: ${e.rid}"))
    case dbe: DBExceptions => Left(GenericDatabaseError(dbe, Some(dbe.rid.id)))
    case t: Throwable => Left(GenericError(t))
  }

  private def toEitherT[T](eventualT: Future[T]) =
    EitherT[Future, LeftState, T](eventualT.map(Right(_)).recover(repositoryErrorHandler))

  private def getOrCreateVatScheme(profile: CurrentProfile): Future[Either[LeftState, VatScheme]] =
    registrationRepository.retrieveVatScheme(RegistrationId(profile.registrationID)).flatMap {
      case Some(vatScheme) => Future.successful(Right(vatScheme))
      case None => registrationRepository.createNewVatScheme(RegistrationId(profile.registrationID))
        .map(Right(_)).recover(repositoryErrorHandler)
    }

  override def createNewRegistration()(implicit headerCarrier: HeaderCarrier): ServiceResult[VatScheme] =
    for {
      profile <- EitherT(brConnector.retrieveCurrentProfile)
      vatScheme <- EitherT(getOrCreateVatScheme(profile))
    } yield vatScheme

  override def retrieveVatScheme(rid: RegistrationId): ServiceResult[VatScheme] =
    EitherT(registrationRepository.retrieveVatScheme(rid).map[Either[LeftState, VatScheme]] {
      case Some(vatScheme) => Right(vatScheme)
      case None => Left(ResourceNotFound(rid.id))
    })

  override def updateLogicalGroup[G: LogicalGroup : Format](rid: RegistrationId, group: G): ServiceResult[G] =
    toEitherT(registrationRepository.updateLogicalGroup(rid, group))

  override def deleteVatScheme(rid: RegistrationId): ServiceResult[Boolean] =
    toEitherT(registrationRepository.deleteVatScheme(rid))

  override def deleteBankAccountDetails(rid: RegistrationId): ServiceResult[Boolean] =
    toEitherT(registrationRepository.deleteBankAccountDetails(rid))

  override def deleteZeroRatedTurnover(rid: RegistrationId): ServiceResult[Boolean] =
    toEitherT(registrationRepository.deleteZeroRatedTurnover(rid))

  override def deleteAccountingPeriodStart(rid: RegistrationId): ServiceResult[Boolean] =
    toEitherT(registrationRepository.deleteAccountingPeriodStart(rid))

}

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
import common.RegistrationId
import common.LogicalGroup
import common.exceptions._
import connectors._
import models._
import models.api.VatScheme
import models.external.CurrentProfile
import play.api.libs.json.Writes
import repositories.RegistrationRepository
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait RegistrationService {

  def createNewRegistration()(implicit headerCarrier: HeaderCarrier): ServiceResult[VatScheme]

  def retrieveVatScheme(id: RegistrationId): ServiceResult[VatScheme]

  def updateLogicalGroup[G: LogicalGroup : Writes](id: RegistrationId, group: G): ServiceResult[G]

  def deleteVatScheme(id: RegistrationId): ServiceResult[Boolean]

  def deleteBankAccountDetails(id: RegistrationId): ServiceResult[Boolean]

  def deleteZeroRatedTurnover(id: RegistrationId): ServiceResult[Boolean]

  def deleteAccountingPeriodStart(id: RegistrationId): ServiceResult[Boolean]

}

class VatRegistrationService @Inject()(brConnector: BusinessRegistrationConnector,
                                       registrationRepository: RegistrationRepository
                                      ) extends RegistrationService {

  import cats.instances.future._

  private def repositoryErrorHandler[T]: PartialFunction[Throwable, Either[LeftState, T]] = {
    case e: MissingRegDocument => Left(ResourceNotFound(s"No registration found for registration ID: ${e.id}"))
    case dbe: DBExceptions => Left(GenericDatabaseError(dbe, Some(dbe.id.value)))
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

  override def retrieveVatScheme(id: RegistrationId): ServiceResult[VatScheme] =
    EitherT(registrationRepository.retrieveVatScheme(id).map[Either[LeftState, VatScheme]] {
      case Some(vatScheme) => Right(vatScheme)
      case None => Left(ResourceNotFound(id.value))
    })

  override def updateLogicalGroup[G: LogicalGroup : Writes](id: RegistrationId, group: G): ServiceResult[G] =
    toEitherT(registrationRepository.updateLogicalGroup(id, group))

  override def deleteVatScheme(id: RegistrationId): ServiceResult[Boolean] =
    toEitherT(registrationRepository.deleteVatScheme(id))

  override def deleteBankAccountDetails(id: RegistrationId): ServiceResult[Boolean] =
    toEitherT(registrationRepository.deleteBankAccountDetails(id))

  override def deleteZeroRatedTurnover(id: RegistrationId): ServiceResult[Boolean] =
    toEitherT(registrationRepository.deleteZeroRatedTurnover(id))

  override def deleteAccountingPeriodStart(id: RegistrationId): ServiceResult[Boolean] =
    toEitherT(registrationRepository.deleteAccountingPeriodStart(id))

}

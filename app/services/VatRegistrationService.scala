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

import javax.inject.{Inject, Singleton}

import cats.data.{EitherT, OptionT}
import cats.instances.FutureInstances
import cats.syntax.ApplicativeSyntax
import common.exceptions._
import common.{LogicalGroup, RegistrationId}
import connectors._
import models.api.VatScheme
import models.external.CurrentProfile
import models.{AcknowledgementReferencePath, ElementPath}
import play.api.libs.json.{JsValue, Json, Writes}
import repositories.{RegistrationMongo, RegistrationRepository}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._

import scala.concurrent.Future

@Singleton
class RegistrationService @Inject()(brConn: BusinessRegistrationConnector,
                                   regMongo: RegistrationMongo) extends VatRegistrationService {
  override val registrationRepository: RegistrationRepository = regMongo.store
  override val brConnector: BusinessRegistrationConnector = brConn
}

trait VatRegistrationService extends ApplicativeSyntax with FutureInstances {

  val registrationRepository : RegistrationRepository
  val brConnector : BusinessRegistrationConnector

  private def repositoryErrorHandler[T]: PartialFunction[Throwable, Either[LeftState, T]] = {
    case e: MissingRegDocument  => Left(ResourceNotFound(s"No registration found for registration ID: ${e.id}"))
    case dbe: DBExceptions      => Left(GenericDatabaseError(dbe, Some(dbe.id.value)))
    case t: Throwable           => Left(GenericError(t))
  }

  private def toEitherT[T](eventualT: Future[T])(implicit hc: HeaderCarrier) =
    EitherT[Future, LeftState, T](eventualT.map(Right(_)).recover(repositoryErrorHandler))

  private def getOrCreateVatScheme(profile: CurrentProfile)(implicit hc: HeaderCarrier): Future[Either[LeftState, VatScheme]] =
    registrationRepository.retrieveVatScheme(RegistrationId(profile.registrationID)).flatMap {
      case Some(vatScheme) => Future.successful(Right(vatScheme))
      case None => registrationRepository.createNewVatScheme(RegistrationId(profile.registrationID))
        .map(Right(_)).recover(repositoryErrorHandler)
    }

  import cats.syntax.either._

  def saveAcknowledgementReference(id: RegistrationId, ackRef: String)(implicit hc: HeaderCarrier): ServiceResult[String] =
    OptionT(registrationRepository.retrieveVatScheme(id)).toRight(ResourceNotFound(s"VatScheme ID: $id missing"))
      .flatMap(vs => vs.acknowledgementReference match {
        case Some(ar) =>
          Left[LeftState, String](AcknowledgementReferenceExists(s"""Registration ID $id already has an acknowledgement reference of: $ar""")).toEitherT
        case None =>
          EitherT.liftT(registrationRepository.updateByElement(id, AcknowledgementReferencePath, ackRef))
      })

  def getStatus(id: RegistrationId)(implicit hc: HeaderCarrier): ServiceResult[JsValue] =
    toEitherT(registrationRepository.retrieveVatScheme(id) map {
      case Some(registration) => {
        val json = Json.obj("status" -> registration.status)
        val ackRef = registration.acknowledgementReference.fold(Json.obj())(ackRef => Json.obj("ackRef" -> ackRef))

        json ++ ackRef
      }
      case None => throw MissingRegDocument(id)
    })

  def createNewRegistration()(implicit headerCarrier: HeaderCarrier): ServiceResult[VatScheme] =
    for {
      profile <- EitherT(brConnector.retrieveCurrentProfile)
      vatScheme <- EitherT(getOrCreateVatScheme(profile))
    } yield vatScheme

  def retrieveVatScheme(id: RegistrationId)(implicit hc: HeaderCarrier): ServiceResult[VatScheme] =
    OptionT(registrationRepository.retrieveVatScheme(id)).toRight(ResourceNotFound(id.value))

  def updateLogicalGroup[G: LogicalGroup : Writes](id: RegistrationId, group: G)(implicit hc: HeaderCarrier): ServiceResult[G] =
    toEitherT(registrationRepository.updateLogicalGroup(id, group))

  def deleteVatScheme(id: RegistrationId)(implicit hc: HeaderCarrier): ServiceResult[Boolean] =
    toEitherT(registrationRepository.deleteVatScheme(id))

  def deleteByElement(id: RegistrationId, elementPath: ElementPath)(implicit hc: HeaderCarrier): ServiceResult[Boolean] =
    toEitherT(registrationRepository.deleteByElement(id, elementPath))

  def retrieveAcknowledgementReference(id: RegistrationId)(implicit hc: HeaderCarrier): ServiceResult[String] =
    retrieveVatScheme(id).subflatMap(_.acknowledgementReference.toRight(ResourceNotFound("AcknowledgementId")))

  def updateIVStatus(regId: String, ivStatus: Boolean)(implicit hc: HeaderCarrier): Future[Boolean] = {
    registrationRepository.updateIVStatus(regId, ivStatus)
  }

  //TODO ResourceNotFound review if appropriate
}

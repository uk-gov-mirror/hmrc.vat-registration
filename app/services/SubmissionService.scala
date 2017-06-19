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

import cats.data.EitherT.liftT
import cats.instances.future._
import common.RegistrationId
import common.exceptions.ResourceNotFound
import config.MicroserviceAuditConnector
import repositories._
import uk.gov.hmrc.play.audit.http.connector.AuditConnector

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.control.NoStackTrace

class RejectedIncorporationException(msg: String) extends NoStackTrace {
  override def getMessage: String = msg
}

@Singleton
class SubmissionService @Inject()(injSequenceMongoRepository: SequenceMongo,
                                  initVatRegistrationService: VatRegistrationService) extends SubmissionSrv {
  val sequenceRepository = injSequenceMongoRepository.store
  val vatRegistrationService = initVatRegistrationService
  val auditConnector = MicroserviceAuditConnector
}

trait SubmissionSrv {

  val sequenceRepository: SequenceRepository
  val vatRegistrationService: VatRegistrationService
  val auditConnector: AuditConnector

  def assertOrGenerateAcknowledgementReference(id: RegistrationId): ServiceResult[String] =
    vatRegistrationService.retrieveAcknowledgementReference(id).recoverWith {
      case _: ResourceNotFound => for {
        generated <- liftT(generateAcknowledgementReference)
        saved <- vatRegistrationService.saveAcknowledgementReference(id, generated)
      } yield saved
    }

  private[services] def generateAcknowledgementReference: Future[String] =
    sequenceRepository.getNext("AcknowledgementID").map(ref => f"BRPY$ref%011d")

}
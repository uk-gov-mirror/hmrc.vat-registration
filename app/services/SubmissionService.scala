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
import cats.instances.FutureInstances
import common.RegistrationId
import common.exceptions.ResourceNotFound
import config.MicroserviceAuditConnector
import org.slf4j.{Logger, LoggerFactory}
import repositories._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._

import scala.concurrent.Future

@Singleton
class SubmissionService @Inject()(val sequenceRepository: SequenceRepository,
                                  val vatRegistrationService: VatRegistrationService) extends SubmissionSrv {
  val auditConnector = MicroserviceAuditConnector
}

trait SubmissionSrv extends FutureInstances {

  val sequenceRepository: SequenceRepository
  val vatRegistrationService: VatRegistrationService
  val auditConnector: AuditConnector

  private val logger: Logger = LoggerFactory.getLogger(getClass)

  def assertOrGenerateAcknowledgementReference(id: RegistrationId)(implicit hc: HeaderCarrier): ServiceResult[String] =
    vatRegistrationService.retrieveAcknowledgementReference(id).recoverWith {
      case _: ResourceNotFound => for {
        generated <- liftT(generateAcknowledgementReference)
        saved <- vatRegistrationService.saveAcknowledgementReference(id, generated)
      } yield {
        logger.info(s"[assertOrGenerateAcknowledgementReference] - There was existing AckRef for regId ${id.value}; generating new AckRef")
        saved
      }
    }

  private[services] def generateAcknowledgementReference(implicit hc: HeaderCarrier): Future[String] =
    sequenceRepository.getNext("AcknowledgementID").map(ref => f"BRVT$ref%011d")

}

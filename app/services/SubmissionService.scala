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

import cats.instances.FutureInstances
import common.exceptions._
import connectors.DESConnector
import enums.VatRegStatus
import javax.inject.{Inject, Singleton}
import models.api.VatSubmission
import repositories._
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SubmissionService @Inject()(val sequenceMongoRepository: SequenceMongoRepository,
                                  val vatRegistrationService: VatRegistrationService,
                                  val registrationRepository: RegistrationMongoRepository,
                                  val desConnector: DESConnector) extends FutureInstances {

  def submitVatRegistration(regId: String)(implicit hc: HeaderCarrier): Future[String] = {
    for {
      status <- getValidDocumentStatus(regId)
      ackRefs <- ensureAcknowledgementReference(regId, status)
      submission <- buildSubmission(regId, ackRefs)
      _ <- desConnector.submitToDES(submission, regId)
      _ <- updateSubmissionStatus(regId)
    } yield {
      ackRefs
    }
  }

  def getAcknowledgementReference(id: String)(implicit hc: HeaderCarrier): ServiceResult[String] =
    vatRegistrationService.retrieveAcknowledgementReference(id)

  private[services] def generateAcknowledgementReference(implicit hc: HeaderCarrier): Future[String] =
    sequenceMongoRepository.getNext("AcknowledgementID").map(ref => f"BRVT$ref%011d")

  private[services] def ensureAcknowledgementReference(regId: String, status: VatRegStatus.Value)
                                                      (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[String] = {
    registrationRepository.retrieveVatScheme(regId) flatMap {
      case Some(vs) => vs.acknowledgementReference.fold(
        for {
          newAckref <- generateAcknowledgementReference
          _ <- registrationRepository.prepareRegistrationSubmission(regId, newAckref, status)
        } yield newAckref
      )(ar => Future.successful(ar))
      case _ => throw new MissingRegDocument(regId)
    }
  }

  private[services] def buildSubmission(regId: String, ackRef: String)
                                       (implicit hc: HeaderCarrier): Future[VatSubmission] = {
    registrationRepository.retrieveVatScheme(regId) map {
      case Some(vatScheme) => VatSubmission.fromVatScheme(vatScheme)
      case _ => throw MissingRegDocument(regId)
    }
  }

  private[services] def getValidDocumentStatus(regID: String)(implicit hc: HeaderCarrier): Future[VatRegStatus.Value] = {
    registrationRepository.retrieveVatScheme(regID) map {
      case Some(registration) => registration.status match {
        case VatRegStatus.draft | VatRegStatus.locked => registration.status
        case _ => throw InvalidSubmissionStatus(s"VAT submission status was in a ${registration.status} state")
      }
      case _ => throw new MissingRegDocument(regID)
    }
  }

  private[services] def updateSubmissionStatus(regId: String)
                                              (implicit hc: HeaderCarrier): Future[VatRegStatus.Value] = {
    registrationRepository.finishRegistrationSubmission(
      regId,
      VatRegStatus.submitted
    )
  }

}

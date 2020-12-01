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
import connectors.VatSubmissionConnector
import enums.VatRegStatus
import javax.inject.{Inject, Singleton}
import models.api.{Submitted, VatSubmission}
import models.monitoring.RegistrationSubmissionAuditing.RegistrationSubmissionAuditModel
import play.api.Logger
import play.api.libs.json.Json
import play.api.mvc.Request
import repositories._
import services.monitoring.AuditService
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals._
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.auth.core.{AuthConnector, AuthorisedFunctions}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import utils.{IdGenerator, TimeMachine}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SubmissionService @Inject()(sequenceMongoRepository: SequenceMongoRepository,
                                  registrationRepository: RegistrationMongoRepository,
                                  vatSubmissionConnector: VatSubmissionConnector,
                                  nonRepudiationService: NonRepudiationService,
                                  trafficManagementService: TrafficManagementService,
                                  timeMachine: TimeMachine,
                                  auditService: AuditService,
                                  idGenerator: IdGenerator,
                                  val authConnector: AuthConnector
                                 )(implicit executionContext: ExecutionContext) extends FutureInstances with AuthorisedFunctions {

  def submitVatRegistration(regId: String, userHeaders: Map[String, String])
                           (implicit hc: HeaderCarrier,
                            request: Request[_]): Future[String] = {
    for {
      status <- getValidDocumentStatus(regId)
      ackRefs <- ensureAcknowledgementReference(regId, status)
      submission <- buildSubmission(regId)
      _ <- submit(submission, regId, userHeaders) // TODO refactor so this returns a value from the VatRegStatus enum or maybe an ADT
      _ <- registrationRepository.finishRegistrationSubmission(regId, VatRegStatus.submitted)
      _ <- trafficManagementService.updateStatus(regId, Submitted)
    } yield {
      ackRefs
    }
  }

  private[services] def submit(submission: VatSubmission,
                               regId: String,
                               userHeaders: Map[String, String]
                              )(implicit hc: HeaderCarrier,
                                request: Request[_]): Future[HttpResponse] = {

    val correlationId = idGenerator.createId
    Logger.info(s"VAT Submission API Correlation Id: $correlationId for the following regId: $regId")

    authorised().retrieve(credentials and affinityGroup and agentCode) {
      case Some(credentials) ~ Some(affinity) ~ optAgentCode =>
        vatSubmissionConnector.submit(submission, correlationId, credentials.providerId).map {
          response =>
            auditService.audit(RegistrationSubmissionAuditModel(
              vatSubmission = submission,
              regId = regId,
              authProviderId = credentials.providerId,
              affinityGroup = affinity,
              optAgentReferenceNumber = optAgentCode
            ))

            val nonRepudiationPayload = Json.toJson(submission).toString

            //TODO - Confirm what to send when postcode is not available
            val nonRepudiationPostcode = submission.businessContact.ppob.postcode.getOrElse("NoPostcodeSupplied")
            nonRepudiationService.submitNonRepudiation(regId, nonRepudiationPayload, timeMachine.timestamp, nonRepudiationPostcode, userHeaders)
            response
        }
    }
  }

  private[services] def ensureAcknowledgementReference(regId: String,
                                                       status: VatRegStatus.Value): Future[String] = {
    registrationRepository.retrieveVatScheme(regId) flatMap {
      case Some(vs) => vs.acknowledgementReference.fold(
        for {
          newAckref <- sequenceMongoRepository.getNext("AcknowledgementID").map(ref => f"BRVT$ref%011d")
          _ <- registrationRepository.prepareRegistrationSubmission(regId, newAckref, status)
        } yield newAckref
      )(ar => Future.successful(ar))
      case _ => throw MissingRegDocument(regId)
    }
  }

  private[services] def buildSubmission(regId: String): Future[VatSubmission] = {
    registrationRepository.retrieveVatScheme(regId) map {
      case Some(vatScheme) => VatSubmission.fromVatScheme(vatScheme)
      case _ => throw MissingRegDocument(regId)
    }
  }

  private[services] def getValidDocumentStatus(regID: String): Future[VatRegStatus.Value] = {
    registrationRepository.retrieveVatScheme(regID) map {
      case Some(registration) => registration.status match {
        case VatRegStatus.draft | VatRegStatus.locked => registration.status
        case _ => throw InvalidSubmissionStatus(s"VAT submission status was in a ${registration.status} state")
      }
      case _ => throw MissingRegDocument(regID)
    }
  }

}

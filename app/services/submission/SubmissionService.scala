/*
 * Copyright 2021 HM Revenue & Customs
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

package services.submission

import cats.instances.FutureInstances
import common.exceptions._
import connectors.VatSubmissionConnector
import enums.VatRegStatus
import featureswitch.core.config.{CheckYourAnswersNrsSubmission, FeatureSwitching, UseSubmissionAuditBuilders}
import models.api.{Submitted, VatScheme}
import models.monitoring.RegistrationSubmissionAuditing.RegistrationSubmissionAuditModel
import models.submission.VatSubmission
import play.api.Logging
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.Request
import repositories._
import services.monitoring.{AuditService, SubmissionAuditBlockBuilder}
import services.{NonRepudiationService, TrafficManagementService}
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals.{credentials, _}
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.auth.core.{AuthConnector, AuthorisedFunctions}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, InternalServerException}
import utils.{IdGenerator, TimeMachine}

import java.util.Base64
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SubmissionService @Inject()(sequenceMongoRepository: SequenceMongoRepository,
                                  registrationRepository: RegistrationMongoRepository,
                                  vatSubmissionConnector: VatSubmissionConnector,
                                  nonRepudiationService: NonRepudiationService,
                                  trafficManagementService: TrafficManagementService,
                                  submissionPayloadBuilder: SubmissionPayloadBuilder,
                                  submissionAuditBlockBuilder: SubmissionAuditBlockBuilder,
                                  timeMachine: TimeMachine,
                                  auditService: AuditService,
                                  idGenerator: IdGenerator,
                                  val authConnector: AuthConnector
                                 )(implicit executionContext: ExecutionContext) extends FutureInstances with AuthorisedFunctions with Logging with FeatureSwitching {

  def submitVatRegistration(regId: String, userHeaders: Map[String, String])
                           (implicit hc: HeaderCarrier,
                            request: Request[_]): Future[String] = {
    for {
      status <- getValidDocumentStatus(regId)
      ackRefs <- ensureAcknowledgementReference(regId, status)
      oldSubmission <- buildOldSubmission(regId)
      vatScheme <- registrationRepository.retrieveVatScheme(regId)
        .map(_.getOrElse(throw new InternalServerException("[SubmissionService][submitVatRegistration] Missing VatScheme")))
      submission <- submissionPayloadBuilder.buildSubmissionPayload(regId)
      _ <- submit(submission, vatScheme, oldSubmission, regId, userHeaders) // TODO refactor so this returns a value from the VatRegStatus enum or maybe an ADT
      _ <- registrationRepository.finishRegistrationSubmission(regId, VatRegStatus.submitted)
      _ <- trafficManagementService.updateStatus(regId, Submitted)
    } yield {
      ackRefs
    }
  }

  // scalastyle:off
  private[services] def submit(submission: JsObject,
                               vatScheme: VatScheme,
                               oldSubmission: VatSubmission,
                               regId: String,
                               userHeaders: Map[String, String]
                              )(implicit hc: HeaderCarrier,
                                request: Request[_]): Future[HttpResponse] = {

    val correlationId = idGenerator.createId
    logger.info(s"VAT Submission API Correlation Id: $correlationId for the following regId: $regId")

    authorised().retrieve(credentials and affinityGroup and agentCode) {
      case Some(credentials) ~ Some(affinity) ~ optAgentCode =>
        vatSubmissionConnector.submit(submission, correlationId, credentials.providerId).map {
          response =>
            if (isEnabled(UseSubmissionAuditBuilders)) {
              auditService.audit(
                submissionAuditBlockBuilder.buildAuditJson(
                  vatScheme = vatScheme,
                  authProviderId = credentials.providerId,
                  affinityGroup = affinity,
                  optAgentReferenceNumber = optAgentCode
                )
              )
            }
            else {
              auditService.audit(RegistrationSubmissionAuditModel(
                vatSubmission = oldSubmission,
                regId = regId,
                authProviderId = credentials.providerId,
                affinityGroup = affinity,
                optAgentReferenceNumber = optAgentCode
              ))
            }

            if (isEnabled(CheckYourAnswersNrsSubmission)) {
              val encodedHtml = vatScheme.nrsSubmissionPayload
                .getOrElse(throw new InternalServerException("[SubmissionService][submit] Missing NRS Submission payload"))
              val payloadString = new String(Base64.getDecoder.decode(encodedHtml))
              val postCode = vatScheme.businessContact
                .getOrElse(throw new InternalServerException("[SubmissionService][submit] Missing business contact details"))
                .ppob.postcode.getOrElse(throw new InternalServerException("[SubmissionService][submit] Missing postcode"))
              //TODO - Confirm what to send when postcode is not available

              nonRepudiationService.submitNonRepudiation(regId, payloadString, timeMachine.timestamp, postCode, userHeaders)
            }
            else {
              val nonRepudiationPostcode = oldSubmission.businessContact.ppob.postcode.getOrElse("NoPostcodeSupplied")
              //TODO - Confirm what to send when postcode is not available

              nonRepudiationService.submitNonRepudiation(regId, Json.toJson(submission).toString, timeMachine.timestamp, nonRepudiationPostcode, userHeaders)
            }

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

  private[services] def buildOldSubmission(regId: String): Future[VatSubmission] = {
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

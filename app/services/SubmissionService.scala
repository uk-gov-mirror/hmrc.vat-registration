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

import java.time.LocalDate
import javax.inject.{Inject, Singleton}

import cats.data.EitherT.liftT
import cats.instances.FutureInstances
import common.exceptions._
import common.{RegistrationId, TransactionId}
import config.MicroserviceAuditConnector
import connectors.{CompanyRegistrationConnector, DESConnector, IncorporationInformationConnector}
import enums.VatRegStatus
import models.api.VatScheme
import models.external.IncorporationStatus
import models.submission.DESSubmission
import play.api.Logger
import repositories._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

@Singleton
class SubmissionService @Inject()(val sequenceRepository: SequenceRepository,
                                  val vatRegistrationService: VatRegistrationService,
                                  val registrationRepository: RegistrationRepository,
                                  val companyRegistrationConnector: CompanyRegistrationConnector,
                                  val desConnector: DESConnector,
                                  val incorporationInformationConnector : IncorporationInformationConnector) extends SubmissionSrv {
  val auditConnector = MicroserviceAuditConnector
}

trait SubmissionSrv extends FutureInstances {

  val sequenceRepository: SequenceRepository
  val vatRegistrationService: VatRegistrationService
  val registrationRepository : RegistrationRepository
  val companyRegistrationConnector : CompanyRegistrationConnector
  val desConnector: DESConnector
  val incorporationInformationConnector: IncorporationInformationConnector
  val auditConnector: AuditConnector

  private val REGIME = "vat"
  private val SUBSCRIBER = "scrs"

  def submitVatRegistration(regId : RegistrationId)(implicit hc : HeaderCarrier) : Future[String] = {
    for {
      _             <- getValidDocumentStatus(regId)
      ackRefs       <- ensureAcknowledgementReference(regId)
      transID       <- fetchCompanyRegistrationTransactionID(regId)
      incorpStatus  <- registerForInterest(transID)
      incorpDate    = getIncorpDate(incorpStatus)
      companyName   <- getCompanyName(regId, TransactionId(transID))
      submission    <- buildDesSubmission(regId, ackRefs, companyName, incorpDate)
      _             <- desConnector.submitToDES(submission, regId.toString)
      _             <- registrationRepository.finishRegistrationSubmission(regId)
    } yield {
      ackRefs
    }
  }

  def getAcknowledgementReference(id: RegistrationId)(implicit hc: HeaderCarrier): ServiceResult[String] =
    vatRegistrationService.retrieveAcknowledgementReference(id)

  private[services] def generateAcknowledgementReference(implicit hc: HeaderCarrier): Future[String] =
    sequenceRepository.getNext("AcknowledgementID").map(ref => f"BRVT$ref%011d")

  private[services] def ensureAcknowledgementReference(regId: RegistrationId)(implicit hc : HeaderCarrier, ec: ExecutionContext): Future[String] = {
    registrationRepository.retrieveVatScheme(regId) flatMap {
      case Some(vs) => vs.acknowledgementReference.fold(
        for {
          newAckref <- generateAcknowledgementReference
          _ <- registrationRepository.prepareRegistrationSubmission(regId, newAckref)
        } yield newAckref
      )(ar => Future.successful(ar))
      case _ => throw MissingRegDocument(regId)
    }
  }

  def retrieveVatStartDate(vatScheme: VatScheme, regId: RegistrationId) : LocalDate = {
    vatScheme.tradingDetails match {
      case Some(td) => td.vatChoice.vatStartDate.startDate match {
        case Some(startDate)  => startDate
        case None             => throw NoVatStartDate(s"Vat start date was not found for regID: $regId")
      }
      case None => throw NoTradingDetails(s"Vat trading details was not found for regID: $regId")
    }
  }

  private[services] def buildDesSubmission(regId: RegistrationId, ackRef: String, companyName: String, incorpDate: LocalDate)
                                          (implicit hc: HeaderCarrier): Future[DESSubmission] = {
    registrationRepository.retrieveVatScheme(regId) map {
      case Some(vs)     => DESSubmission(ackRef, companyName, retrieveVatStartDate(vs, regId), incorpDate)
      case None         => throw MissingRegDocument(regId)
    }
  }

  private[services] def getValidDocumentStatus(regID : RegistrationId)(implicit hc : HeaderCarrier) : Future[String] = {
    registrationRepository.retrieveVatScheme(regID) map {
      case Some(registration) => registration.status match {
        case VatRegStatus.draft => registration.status.toString
        case _                  => throw InvalidSubmissionStatus(s"VAT submission status was in a ${registration.status} state")
      }
      case None => throw MissingRegDocument(regID)
    }
  }

  private[services] def fetchCompanyRegistrationTransactionID(regId: RegistrationId)(implicit hc: HeaderCarrier): Future[String] = {
    companyRegistrationConnector.fetchCompanyRegistrationDocument(regId) map { response =>
      Try((response.json \ "confirmationReferences" \ "transaction-id").as[String]) match {
        case Success(transactionID) => transactionID
        case Failure(_)             => throw NoTransactionId(s"Fetching of transaction id failed for regId: $regId")
      }
    }
  }

  private[services] def registerForInterest(transID: String)(implicit hc: HeaderCarrier): Future[IncorporationStatus] = {
    incorporationInformationConnector.retrieveIncorporationStatus(TransactionId(transID), REGIME, SUBSCRIBER) map {
      case Some(incStatus)  => incStatus
      case None             => throw NoIncorpUpdate(s"Retrieve of vat incorporation update did not bring back an incorp update with " +
        s"transId: $transID, regime: $REGIME, subsciber: $SUBSCRIBER")
    }
  }

  private[services] def getIncorpDate(incorpStatus: IncorporationStatus): LocalDate = {
    incorpStatus.statusEvent.incorporationDate match {
      case Some(incorpDate) => incorpDate
      case None             => throw NoIncorpDate(s"No incorp date found in the incorpStatus for transID: ${incorpStatus.subscription.transactionId}")
    }
  }

  private[services] def getCompanyName(regId: RegistrationId, txId: TransactionId)(implicit hc: HeaderCarrier): Future[String] = {
    incorporationInformationConnector.getCompanyName(regId, txId) map { resp =>
      Try((resp.json \ "company_name").as[String]) match {
        case Success(companyName)   => companyName
        case Failure(_)             => throw NoCompanyName(s"Could not retrieve company name from II with regId: $regId and transId: $txId")
      }
    }
  }
}

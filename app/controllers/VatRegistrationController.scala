/*
 * Copyright 2019 HM Revenue & Customs
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

package controllers

import auth.Authorisation
import cats.instances.FutureInstances
import common.RegistrationId
import common.exceptions.{InvalidSubmissionStatus, LeftState}
import enums.VatRegStatus
import javax.inject.Inject
import models.api._
import play.api.Logger
import play.api.libs.json._
import play.api.mvc._
import repositories.{RegistrationMongo, RegistrationMongoRepository}
import services._
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.play.bootstrap.controller.BaseController
import scala.concurrent.ExecutionContext.Implicits.global

class VatRegistrationControllerImpl @Inject()(val registrationService: RegistrationService,
                                              val submissionService: SubmissionService,
                                              val registrationMongo: RegistrationMongo,
                                              val authConnector: AuthConnector) extends VatRegistrationController {

  val registrationRepository: RegistrationMongoRepository = registrationMongo.store
  val resourceConn = submissionService.registrationRepository


}

trait VatRegistrationController extends BaseController with Authorisation with FutureInstances {

  val authConnector: AuthConnector
  val registrationService: RegistrationService
  val submissionService: SubmissionService
  val registrationRepository: RegistrationMongoRepository

  val errorHandler: (LeftState) => Result = err => err.toResult

  def newVatRegistration: Action[AnyContent] = Action.async {
    implicit request =>
      isAuthenticated { internalId =>
        implicit val writes = VatScheme.apiWrites
        registrationService.createNewRegistration(internalId)
          .fold(errorHandler, vatScheme => Created(Json.toJson(vatScheme)))
      }
  }

  def retrieveVatScheme(id: RegistrationId): Action[AnyContent] = Action.async {
    implicit request =>
      isAuthorised(id.value) { authResult =>
        authResult.ifAuthorised(id.value, "VatRegistrationController", "retrieveVatScheme") {
          implicit val writes = VatScheme.apiWrites
          registrationService.retrieveVatScheme(id).fold(errorHandler, vatScheme => Ok(Json.toJson(vatScheme)))
        }
      }
  }

// TODO: this returns 404 when other methods return 204. Refactor to return 204 at some point
  def fetchReturns(regId: String) : Action[AnyContent] = Action.async {
    implicit request =>
      isAuthorised(regId) { authResult =>
        authResult.ifAuthorised(regId, "VatRegistrationController", "fetchReturns") {
          registrationRepository.fetchReturns(regId) map {
            case Some(returns) => Ok(Json.toJson(returns))
            case None => NotFound
          }
        }
      }
  }

  def updateReturns(regId: String): Action[JsValue] = Action.async(parse.json) {
    implicit request =>
      isAuthorised(regId) { authResult =>
        authResult.ifAuthorised(regId, "VatRegistrationController", "updateReturns") {
          withJsonBody[Returns] { returns =>
            registrationRepository.updateReturns(regId, returns) map (_ => Ok)
          }
        }
      }
  }

  def submitVATRegistration(id: RegistrationId) : Action[AnyContent] = Action.async {
    implicit request =>
      isAuthorised(id.value) { authResult =>
        authResult.ifAuthorised(id.value, "VatRegistrationController", "submitVATRegistration") {
            submissionService.submitVatRegistration(id).map { ackRefs =>
              Ok(Json.toJson(ackRefs))
            } recover {
              case ex =>
                Logger.warn(s"Submission failed - ${ex.getMessage}")
                throw ex
            }
        }
      }
  }

  def getAcknowledgementReference(id: RegistrationId): Action[AnyContent] = Action.async {
    implicit request =>
      isAuthorised(id.value) { authResult =>
        authResult.ifAuthorised(id.value, "VatRegistrationController", "getAcknowledgementReference") {
            submissionService.getAcknowledgementReference(id).fold(errorHandler, ackRefNumber => Ok(Json.toJson(ackRefNumber)))
        }
      }
  }

  def getTurnoverEstimates(regId: String): Action[AnyContent] = Action.async {
    implicit request =>
      isAuthorised(regId) { authResult =>
        authResult.ifAuthorised(regId, "VatRegistrationController", "getTurnoverEstimate") {
          implicit val reads = TurnoverEstimates.eligibilityDataJsonReads
          registrationService.getBlockFromEligibilityData[TurnoverEstimates](regId) sendResult("getTurnoverEstimates", regId)
        }
      }
  }

  def getThreshold(regId: String): Action[AnyContent] = Action.async {
    implicit request =>
      isAuthorised(regId) { authResult =>
        authResult.ifAuthorised(regId, "VatRegistrationController", "getThreshold") {
          implicit val reads = Threshold.eligibilityDataJsonReads
          registrationService.getBlockFromEligibilityData[Threshold](regId) sendResult("getThreshold", regId)
        }
      }
  }

  def deleteVatScheme(regId: String): Action[AnyContent] = Action.async {
    implicit request =>
      isAuthorised(regId) { authResult =>
        authResult.ifAuthorised(regId, "VatRegistrationController", "deleteVatScheme") {
          registrationService.deleteVatScheme(regId, VatRegStatus.draft, VatRegStatus.rejected) map { deleted =>
            if (deleted) Ok else InternalServerError
          } recover {
            case _: InvalidSubmissionStatus => PreconditionFailed
          }
        }
      }
  }

  def clearDownDocument(transId: String): Action[AnyContent] = Action.async {
    implicit request =>
      registrationService.clearDownDocument(transId).map {
        case true => Ok
        case _ => InternalServerError
      }
  }

  // TODO: this returns 404 when other methods return 204. Refactor to return 204 at some point
  def fetchBankAccountDetails(regId: String): Action[AnyContent] = Action.async {
    implicit request =>
      isAuthorised(regId) { authResult =>
        authResult.ifAuthorised(regId, "VatRegistrationController", "fetchBankAccountDetails") {
          registrationRepository.fetchBankAccount(regId) map {
            case Some(bankAccount) => Ok(Json.toJson(bankAccount))
            case None => NotFound
          }
        }
      }
  }

  def updateBankAccountDetails(regId: String): Action[JsValue] = Action.async(parse.json) {
    implicit request =>
      isAuthorised(regId) { authResult =>
        authResult.ifAuthorised(regId, "VatRegistrationController", "updateBankAccountDetails") {
          withJsonBody[BankAccount] { bankAccount =>
            registrationRepository.updateBankAccount(regId, bankAccount)
              .sendResult("updateBackAccountDetails",regId)
          }
        }
      }
  }

  def getDocumentStatus(id: RegistrationId): Action[AnyContent] = Action.async {
    implicit request =>
      isAuthorised(id.value) { authResult =>
        authResult.ifAuthorised(id.value, "VatRegistrationController", "getDocumentStatus") {
          registrationService.getStatus(id).sendResult("getDocumentStatus", id.value)
        }
      }
  }

  def saveTransId(id: String): Action[JsValue] = Action.async(parse.json) {
    implicit request =>
      withJsonBody[JsValue] { json =>
        val transId: String = (json \ "transactionID").as[String]
        registrationRepository.saveTransId(transId, RegistrationId(id)) map { _ =>
          Ok
        }
      }
  }
}
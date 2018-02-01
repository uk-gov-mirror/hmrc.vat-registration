/*
 * Copyright 2018 HM Revenue & Customs
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

import javax.inject.Inject

import common.RegistrationId
import common.exceptions.{InvalidSubmissionStatus, LeftState, MissingRegDocument}
import connectors.AuthConnector
import enums.VatRegStatus
import models.ElementPath
import models.api._
import play.api.libs.json._
import play.api.mvc._
import repositories.{RegistrationMongo, RegistrationMongoRepository}
import services._
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._
import utils.VATFeatureSwitches

import scala.concurrent.Future

class VatRegistrationControllerImpl @Inject()(val auth: AuthConnector,
                                          val registrationService: RegistrationService,
                                          val submissionService: SubmissionService,
                                          val registrationMongo: RegistrationMongo) extends VatRegistrationController {
  val registrationRepository: RegistrationMongoRepository = registrationMongo.store
  private[controllers] override def useMockSubmission: Boolean = VATFeatureSwitches.mockSubmission.enabled
}

trait VatRegistrationController extends VatRegistrationBaseController {

  val auth: AuthConnector
  val registrationService: RegistrationService
  val submissionService: SubmissionService
  val registrationRepository: RegistrationMongoRepository

  private[controllers] def useMockSubmission: Boolean

  val errorHandler: (LeftState) => Result = err => err.toResult

  def newVatRegistration: Action[AnyContent] = Action.async {
    implicit request =>
      authenticated { _ =>
        implicit val writes = VatScheme.apiWrites
        registrationService.createNewRegistration.fold(errorHandler, vatScheme => Created(Json.toJson(vatScheme)))
      }
  }

  def retrieveVatScheme(id: RegistrationId): Action[AnyContent] = Action.async {
    implicit request =>
      authenticated { _ =>
        implicit val writes = VatScheme.apiWrites
        registrationService.retrieveVatScheme(id).fold(errorHandler, vatScheme => Ok(Json.toJson(vatScheme)))
      }
  }

  @deprecated
  def updateVatFinancials(id: RegistrationId): Action[JsValue] = {
    implicit val format: Format[VatFinancials] = VatFinancials.format
    patch[VatFinancials](registrationService, id)
  }

  def fetchReturns(regId: String) : Action[AnyContent] = Action.async {
    implicit request =>
      registrationRepository.fetchReturns(regId) map {
        case Some(returns) => Ok(Json.toJson(returns))
        case None          => NotFound
      }
  }

  def updateReturns(regId: String): Action[JsValue] = Action.async(parse.json) {
    implicit request =>
      authenticated { _ =>
        withJsonBody[Returns]{ returns =>
          registrationRepository.updateReturns(regId, returns) map ( _ => Ok)
        }
      }
  }

  def fetchBankAccountDetails(regId: String): Action[AnyContent] = Action.async {
    implicit request =>
      registrationRepository.fetchBankAccount(regId) map {
        case Some(bankAccount) => Ok(Json.toJson(bankAccount))
        case None              => NotFound
      }
  }

  def updateBankAccountDetails(regId: String): Action[JsValue] = Action.async(parse.json) {
    implicit request =>
      authenticated { _ =>
        withJsonBody[BankAccount]{ bankAccount =>
          registrationRepository.updateBankAccount(regId, bankAccount) map ( _ => Ok(Json.toJson(bankAccount)))
        }
      }
  }

  def fetchTurnoverEstimates(regId: String): Action[AnyContent] = Action.async {
    implicit request =>
      authenticated { _ =>
        registrationRepository.fetchTurnoverEstimates(regId) map {
          case Some(turnoverEstimates) => Ok(Json.toJson(turnoverEstimates))
          case None                    => NoContent
        } recover {
          case _: MissingRegDocument   => NotFound
        }
      }
  }

  def updateTurnoverEstimates(regId: String): Action[JsValue] = Action.async(parse.json) {
    implicit request =>
      authenticated { _ =>
        withJsonBody[TurnoverEstimates]{ turnoverEstimates =>
          registrationRepository.updateTurnoverEstimates(regId, turnoverEstimates) map ( _ => Ok)
        }
      }
  }

  def updateSicAndCompliance(id: RegistrationId): Action[JsValue] = patch[VatSicAndCompliance](registrationService, id)

  def updateVatContact(id: RegistrationId): Action[JsValue] = patch[VatContact](registrationService, id)

  @deprecated("Use updateEligibility or updateThreshold instead", "12/12/2017")
  def updateVatEligibility(id: RegistrationId): Action[JsValue] = patch[VatServiceEligibility](registrationService, id)

  def updateLodgingOfficer(id: RegistrationId): Action[JsValue] = patch[LodgingOfficer](registrationService, id)

  def submitVATRegistration(id: RegistrationId) : Action[AnyContent] = Action.async {
    implicit request =>
      authenticated { _ =>
        if (!useMockSubmission) {
          submissionService.submitVatRegistration(id).map { ackRefs =>
            Ok(Json.toJson(ackRefs))
          } recover {
            case ex => BadRequest(s"Registration was submitted without full data: ${ex.getMessage}")
          }
        } else {
          Future.successful(Ok(Json.toJson("BRVT000000" + id)))
        }
      }
  }

  def getAcknowledgementReference(id: RegistrationId): Action[AnyContent] = Action.async {
    implicit request =>
      authenticated { _ =>
        if (!useMockSubmission) {
          submissionService.getAcknowledgementReference(id).fold(errorHandler, ackRefNumber => Ok(Json.toJson(ackRefNumber)))
        } else {
          Future.successful(Ok(Json.toJson("BRVT000000" + id)))
        }
      }
  }

  def deleteVatScheme(regId: String): Action[AnyContent] = Action.async {
    implicit request =>
      authenticated { _ =>
        registrationService.deleteVatScheme(regId, VatRegStatus.draft, VatRegStatus.rejected) map { deleted =>
          if(deleted) Ok else InternalServerError
        } recover {
          case _: InvalidSubmissionStatus => PreconditionFailed
        }
      }
  }

  def getDocumentStatus(id: RegistrationId): Action[AnyContent] = Action.async {
    implicit request =>
      authenticated { _ =>
        registrationService.getStatus(id) map { statusJson =>
          Ok(statusJson)
        } recover {
          case _: MissingRegDocument => NotFound
          case e                     =>
            logger.error(s"[getDocumentStatus] - There was a problem getting the document status for regId: ${id.value}", e)
            InternalServerError
        }
      }
  }


  def deleteByElement(id: RegistrationId, elementPath: ElementPath): Action[AnyContent] = delete(registrationService, id, elementPath)

  @deprecated("Use LodgingOfficerController.updateIVStatus instead", "SCRS-9379")
  def updateIVStatus(regId: String): Action[JsValue] = Action.async(parse.json) {
    implicit request =>
      authenticated { _ =>
        withJsonBody[JsValue] { json =>
          registrationService.updateIVStatus(regId, json.\("ivPassed").as[Boolean]) map { _ =>
            Ok(json)
          } recover {
            case e =>
              logger.error(s"[VatRegistrationController] - [updateIVStatus] - There was a problem updating the IV status for regId $regId - err: ${e.getMessage}")
              InternalServerError
          }
        }
      }
  }
}

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

package controllers

import javax.inject.Inject

import cats.instances.future._
import common.RegistrationId
import common.exceptions.LeftState
import connectors.AuthConnector
import models.ElementPath
import models.api._
import play.api.libs.json._
import play.api.mvc.{Action, AnyContent, Result}
import repositories.RegistrationMongoFormats.encryptedFinancials
import services._

import scala.concurrent.ExecutionContext.Implicits.global

class VatRegistrationController @Inject()(val auth: AuthConnector,
                                          registrationService: RegistrationService,
                                          submissionService: SubmissionService)
  extends VatRegistrationBaseController {

  val errorHandler: (LeftState) => Result = err => err.toResult

  def newVatRegistration: Action[AnyContent] = Action.async {
    implicit request =>
      authenticated { _ =>
        registrationService.createNewRegistration.fold(errorHandler, vatScheme => Created(Json.toJson(vatScheme)))
      }
  }

  def retrieveVatScheme(id: RegistrationId): Action[AnyContent] = Action.async {
    implicit request =>
      authenticated { _ =>
        registrationService.retrieveVatScheme(id).fold(errorHandler, vatScheme => Ok(Json.toJson(vatScheme)))
      }
  }

  def updateVatFinancials(id: RegistrationId): Action[JsValue] = {
    implicit val format: Format[VatFinancials] = Format(VatFinancials.format, encryptedFinancials)
    patch[VatFinancials](registrationService, id)
  }

  def updateTradingDetails(id: RegistrationId): Action[JsValue] = patch[VatTradingDetails](registrationService, id)

  def updateSicAndCompliance(id: RegistrationId): Action[JsValue] = patch[VatSicAndCompliance](registrationService, id)

  def updateVatContact(id: RegistrationId): Action[JsValue] = patch[VatContact](registrationService, id)

  def updateVatEligibility(id: RegistrationId): Action[JsValue] = patch[VatServiceEligibility](registrationService, id)

  def updateLodgingOfficer(id: RegistrationId): Action[JsValue] = patch[VatLodgingOfficer](registrationService, id)

  def updatePpob(id: RegistrationId): Action[JsValue] = patch[ScrsAddress](registrationService, id)

  def getAcknowledgementReference(id: RegistrationId): Action[AnyContent] = Action.async {
    implicit request =>
      authenticated { _ =>
        submissionService.assertOrGenerateAcknowledgementReference(id).fold(errorHandler, ackRefNumber => Ok(Json.toJson(ackRefNumber)))
      }
  }

  def deleteVatScheme(id: RegistrationId): Action[AnyContent] = Action.async {
    implicit request =>
      authenticated { _ =>
        registrationService.deleteVatScheme(id).fold(errorHandler, removed => Ok(Json.toJson(removed)))
      }
  }

  def deleteByElement(id: RegistrationId, elementPath: ElementPath): Action[AnyContent] = delete(registrationService, id, elementPath)

}

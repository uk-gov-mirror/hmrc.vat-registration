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

import cats.implicits._
import common.exceptions.LeftState
import connectors.AuthConnector
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action, AnyContent, Result}
import services._

import scala.concurrent.ExecutionContext.Implicits.global

class VatRegistrationController @Inject()(val auth: AuthConnector, registrationService: RegistrationService)
  extends VatRegistrationBaseController {
  val errorHandler: (LeftState) => Result = err => err.toResult

  def newVatRegistration: Action[AnyContent] = Action.async {
    implicit request =>
      authenticated { _ =>
        registrationService.createNewRegistration.fold(errorHandler, vatScheme => Created(Json.toJson(vatScheme)))
      }
  }

  def retrieveVatScheme(regId: String): Action[AnyContent] = Action.async {
    implicit request =>
      authenticated { _ =>
        registrationService.retrieveVatScheme(regId).fold(
          errorHandler,
          vatScheme =>
            Ok(Json.toJson(vatScheme)))
      }
  }

  def updateTradingDetails(regId: String): Action[JsValue] = patch(registrationService.updateTradingDetails, regId)

  def updateVatChoice(regId: String): Action[JsValue] = patch(registrationService.updateVatChoice, regId)

  def updateVatFinancials(regId: String): Action[JsValue] = patch(registrationService.updateVatFinancials, regId)

  def updateSicAndCompliance(regId: String): Action[JsValue] = patch(registrationService.updateSicAndCompliance, regId)

  def deleteVatScheme(regId: String): Action[AnyContent] = Action.async {
    implicit request =>
      authenticated { _ =>
        registrationService.deleteVatScheme(regId).fold(errorHandler, removed => Ok(Json.toJson(removed)))
      }
  }

  def deleteBankAccountDetails(regId: String): Action[AnyContent] = delete(registrationService.deleteBankAccountDetails, regId)

  def deleteZeroRatedTurnover(regId: String): Action[AnyContent] = delete(registrationService.deleteZeroRatedTurnover, regId)

  def deleteAccountingPeriodStart(regId: String): Action[AnyContent] = delete(registrationService.deleteAccountingPeriodStart, regId)

}

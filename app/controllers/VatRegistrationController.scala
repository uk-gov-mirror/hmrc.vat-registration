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
import models.{VatChoice, VatFinancials, VatSicAndCompliance, VatTradingDetails}
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

  def retrieveVatScheme(rid: String): Action[AnyContent] = Action.async {
    implicit request =>
      authenticated { _ =>
        registrationService.retrieveVatScheme(rid).fold(
          errorHandler,
          vatScheme =>
            Ok(Json.toJson(vatScheme)))
      }
  }

  def updateTradingDetails(rid: String): Action[JsValue] = patch[VatTradingDetails](registrationService, rid)

  def updateVatChoice(rid: String): Action[JsValue] = patch[VatChoice](registrationService, rid)

  def updateVatFinancials(rid: String): Action[JsValue] = patch[VatFinancials](registrationService, rid)

  def updateSicAndCompliance(rid: String): Action[JsValue] = patch[VatSicAndCompliance](registrationService, rid)

  def deleteVatScheme(rid: String): Action[AnyContent] = Action.async {
    implicit request =>
      authenticated { _ =>
        registrationService.deleteVatScheme(rid).fold(errorHandler, removed => Ok(Json.toJson(removed)))
      }
  }

  def deleteBankAccountDetails(rid: String): Action[AnyContent] = delete(registrationService.deleteBankAccountDetails, rid)

  def deleteZeroRatedTurnover(rid: String): Action[AnyContent] = delete(registrationService.deleteZeroRatedTurnover, rid)

  def deleteAccountingPeriodStart(rid: String): Action[AnyContent] = delete(registrationService.deleteAccountingPeriodStart, rid)

}

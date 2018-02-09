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

import auth.{Authorisation, AuthorisationResource}
import common.exceptions.MissingRegDocument
import config.AuthClientConnector

import models.api.TradingDetails
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action, AnyContent}
import services.TradingDetailsService
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._
import uk.gov.hmrc.play.microservice.controller.BaseController

class TradingDetailsControllerImpl @Inject()(val tradingDetailsService: TradingDetailsService) extends TradingDetailsController {
  val resourceConn: AuthorisationResource                              = tradingDetailsService.registrationRepository
  override lazy val authConnector: AuthConnector                       = AuthClientConnector
}

trait TradingDetailsController extends BaseController with Authorisation {

  val tradingDetailsService: TradingDetailsService

  def fetchTradingDetails(regId: String): Action[AnyContent] = Action.async {
    implicit request =>
      isAuthenticated { _ =>
        tradingDetailsService.retrieveTradingDetails(regId) sendResult
      }
  }

  def updateTradingDetails(regId: String): Action[JsValue] = Action.async(parse.json) {
    implicit request =>
      isAuthenticated { _ =>
        withJsonBody[TradingDetails]{ tradingDetails =>
          tradingDetailsService.updateTradingDetails(regId, tradingDetails) map {
            tdResponse => Ok(Json.toJson(tdResponse))
          } recover {
            case _: MissingRegDocument =>
              NotFound(s"[TradingDetailsController] [updateTradingDetails] Registration not found for regId: $regId")
            case e =>
              InternalServerError(s"[TradingDetailsController] [updateTradingDetails] " +
              s"An error occurred while updating trading details: for regId: $regId, ${e.getMessage}")
          }
        }
      }
  }
}

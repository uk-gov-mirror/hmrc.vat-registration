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

import auth.Authorisation
import common.exceptions.MissingRegDocument
import config.AuthClientConnector
import models.api.FlatRateScheme
import play.api.Logger
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action, AnyContent}
import services.FlatRateSchemeService
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._
import uk.gov.hmrc.play.microservice.controller.BaseController

class FlatRateSchemeControllerImpl @Inject()(val flatRateSchemeService: FlatRateSchemeService) extends FlatRateSchemeController {

  val resourceConn = flatRateSchemeService.registrationRepository
  override lazy val authConnector:AuthConnector = AuthClientConnector

}

trait FlatRateSchemeController extends BaseController with Authorisation {

  val flatRateSchemeService: FlatRateSchemeService

  def fetchFlatRateScheme(regId: String): Action[AnyContent] = Action.async {
    implicit request =>
      isAuthenticated { _ =>
        flatRateSchemeService.retrieveFlatRateScheme(regId) sendResult
      }
  }

  def updateFlatRateScheme(regId: String): Action[JsValue] = Action.async(parse.json) {
    implicit request =>
      isAuthenticated { _ =>
        withJsonBody[FlatRateScheme]{ flatRateScheme =>
          flatRateSchemeService.updateFlatRateScheme(regId, flatRateScheme) map {
            frsResponse => Ok(Json.toJson(frsResponse))
          } recover {
            case mrd: MissingRegDocument =>
              Logger.error(s"[FlatRateSchemeController] [updateFlatRateScheme] Registration not found for regId: $regId", mrd)
              NotFound
            case e =>
              Logger.error(s"[FlatRateSchemeController] [updateFlatRateScheme] " +
                s"An error occurred while updating flat rate scheme: for regId: $regId, ${e.getMessage}", e)
              InternalServerError
          }
        }
      }
  }

  def removeFlatRateScheme(regId: String): Action[AnyContent] = Action.async {
    implicit request =>
      isAuthenticated { _ =>
        flatRateSchemeService.removeFlatRateScheme(regId) map { result =>
          Ok
        } recover {
          case mrd: MissingRegDocument =>
            Logger.error(s"[FlatRateSchemeController] [removeFlatRateScheme] Registration not found for regId: $regId", mrd)
            NotFound
          case e =>
            Logger.error(s"[FlatRateSchemeController] [removeFlatRateScheme] " +
              s"An error occurred while remove flat rate scheme: for regId: $regId, ${e.getMessage}", e)
            InternalServerError
        }
      }
  }
}

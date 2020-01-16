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

package controllers

import auth.Authorisation
import common.exceptions.MissingRegDocument
import javax.inject.Inject
import models.api.FlatRateScheme
import play.api.Logger
import play.api.libs.json.JsValue
import play.api.mvc.{Action, AnyContent}
import services.FlatRateSchemeService
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.play.bootstrap.controller.BaseController
import scala.concurrent.ExecutionContext.Implicits.global

class FlatRateSchemeControllerImpl @Inject()(val flatRateSchemeService: FlatRateSchemeService,
                                             val authConnector: AuthConnector) extends FlatRateSchemeController {

  val resourceConn = flatRateSchemeService.registrationRepository
}

trait FlatRateSchemeController extends BaseController with Authorisation {

  val flatRateSchemeService: FlatRateSchemeService

  def fetchFlatRateScheme(regId: String): Action[AnyContent] = Action.async {
    implicit request =>
      isAuthorised(regId) { authResult =>
        authResult.ifAuthorised(regId, "FlatRateSchemeController", "fetchFlatRateScheme") {
          flatRateSchemeService.retrieveFlatRateScheme(regId) sendResult("fetchFlatRateScheme", regId)
        }
      }
  }

  def updateFlatRateScheme(regId: String): Action[JsValue] = Action.async(parse.json) {
    implicit request =>
      isAuthorised(regId) { authResult =>
        authResult.ifAuthorised(regId, "FlatRateSchemeController", "updateFlatRateScheme") {
          withJsonBody[FlatRateScheme] { flatRateScheme =>
            flatRateSchemeService.updateFlatRateScheme(regId, flatRateScheme) sendResult("updateFlatRateScheme",regId)
          }
        }
      }
  }

  def removeFlatRateScheme(regId: String): Action[AnyContent] = Action.async {
    implicit request =>
      isAuthorised(regId) { authResult =>
        authResult.ifAuthorised(regId, "FlatRateSchemeController", "removeFlatRateScheme") {
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
}

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
import models.api.Threshold
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action, AnyContent}
import services.ThresholdService
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._
import uk.gov.hmrc.play.microservice.controller.BaseController

class ThresholdControllerImpl @Inject()(val thresholdService: ThresholdService) extends ThresholdController{
  val resourceConn: AuthorisationResource                              = thresholdService.registrationRepository
  override lazy val authConnector: AuthConnector                       = AuthClientConnector
}

trait ThresholdController extends BaseController with Authorisation {

  val thresholdService: ThresholdService

  def getThreshold(regId: String): Action[AnyContent] = Action.async {
    implicit request =>
      isAuthenticated { _ =>
        thresholdService.getThreshold(regId) sendResult
      }
  }

  def updateThreshold(regId: String): Action[JsValue] = Action.async[JsValue](parse.json) {
    implicit request =>
      isAuthenticated {
        context =>
          withJsonBody[Threshold] { threshold =>
            thresholdService.upsertThreshold(regId, threshold) map {
              thresholdResponse => Ok(Json.toJson(thresholdResponse))
            } recover {
              case _: MissingRegDocument => NotFound(s"Registration not found for regId: $regId")
              case e => InternalServerError(s"An error occurred while updating threshold: ${e.getMessage}")
            }
          }
      }
  }
}

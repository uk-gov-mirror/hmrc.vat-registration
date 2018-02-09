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

import models.api.Eligibility
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action, AnyContent}
import services.EligibilityService
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._
import uk.gov.hmrc.play.microservice.controller.BaseController

class EligibilityControllerImpl @Inject()(val eligibilityService: EligibilityService) extends EligibilityController {

  val resourceConn = eligibilityService.registrationRepository
  override lazy val authConnector:AuthConnector = AuthClientConnector
}

trait EligibilityController extends BaseController with Authorisation {

  val eligibilityService: EligibilityService

  def getEligibility(regId: String): Action[AnyContent] = Action.async {
    implicit request =>
      isAuthenticated { authority =>
        eligibilityService.getEligibility(regId) sendResult
      }
  }

  def updateEligibility(regId: String): Action[JsValue] = Action.async[JsValue](parse.json) {
    implicit request =>
      isAuthenticated {
        context =>
          withJsonBody[Eligibility] { eligibility =>
            eligibilityService.upsertEligibility(regId, eligibility) map {
              eligibilityResponse => Ok(Json.toJson(eligibilityResponse))
            } recover {
              case _: MissingRegDocument => NotFound(s"Registration not found for regId: $regId")
              case e => InternalServerError(s"An error occurred while updating eligibility: ${e.getMessage}")
            }
          }
      }
  }
}

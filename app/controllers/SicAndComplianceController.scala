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
import play.api.mvc.{Action, AnyContent}
import services.SicAndComplianceService
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._
import models.api.SicAndCompliance
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.play.microservice.controller.BaseController

class SicAndComplianceControllerImpl @Inject()(val sicAndComplianceService: SicAndComplianceService) extends SicAndComplianceController {
  val resourceConn: AuthorisationResource                              = sicAndComplianceService.registrationRepository
  override lazy val authConnector: AuthConnector                       = AuthClientConnector
}

trait SicAndComplianceController extends BaseController with Authorisation {
  val sicAndComplianceService: SicAndComplianceService

  def getSicAndCompliance(regId: String): Action[AnyContent] = Action.async {
    implicit request =>
      isAuthorised(regId) { authResult =>
        authResult.ifAuthorised(regId, "SicAndComplianceController", "getSicAndCompliance") {
          sicAndComplianceService.getSicAndCompliance(regId) sendResult("getSicAndCompliance", regId)
        }
      }
  }

  def updateSicAndCompliance(regId: String) = Action.async[JsValue](parse.json) {
    implicit request =>
      isAuthorised(regId) { authResult =>
        authResult.ifAuthorised(regId, "SicAndComplianceController", "updateSicAndCompliance") {
          withJsonBody[SicAndCompliance] { sicAndComp =>
            sicAndComplianceService.updateSicAndCompliance(regId, sicAndComp)
              .sendResult("updateSicAndCompliance", regId)
          }
        }
      }
  }
}

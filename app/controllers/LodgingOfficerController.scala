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
import models.api.LodgingOfficer
import play.api.libs.json.{JsBoolean, JsObject, JsValue, Json}
import play.api.mvc.{Action, AnyContent}
import services.LodgingOfficerService
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._
import uk.gov.hmrc.play.microservice.controller.BaseController

class LodgingOfficerControllerImpl @Inject()(val lodgingOfficerService: LodgingOfficerService) extends LodgingOfficerController{
  val resourceConn: AuthorisationResource                              = lodgingOfficerService.registrationRepository
  override lazy val authConnector: AuthConnector                       = AuthClientConnector
}

trait LodgingOfficerController extends BaseController with Authorisation {

  val lodgingOfficerService: LodgingOfficerService

  def updateIVStatus(regId: String, ivPassed: Boolean): Action[AnyContent] = Action.async {
    implicit request =>
      isAuthorised(regId) { authResult =>
        authResult.ifAuthorised(regId, "LodgingOfficerController", "updateIVStatus") {
          lodgingOfficerService.updateIVStatus(regId, ivPassed) map { _ =>
            Ok(JsBoolean(ivPassed))
          } recover {
            case _: MissingRegDocument => NotFound(s"Registration not found or the registration does no have lodgingOfficer defined for regId: $regId")
            case e => InternalServerError(s"An error occurred while updating lodging officer - ivPassed: ${e.getMessage}")
          }
        }
      }
  }

  def getLodgingOfficerData(regId: String): Action[AnyContent] = Action.async {
    implicit request =>
      isAuthorised(regId) { authResult =>
        authResult.ifAuthorised(regId, "LodgingOfficerController", "getLodgingOfficerData") {
          lodgingOfficerService.getLodgingOfficerData(regId) sendResult("getLodgingOfficerData", regId)
        }
      }
  }

  def updateLodgingOfficerData(regId: String): Action[JsValue] = Action.async[JsValue](parse.json) {
    implicit request =>
      isAuthorised(regId) { authResult =>
        authResult.ifAuthorised(regId, "LodgingOfficerController", "updateLodgingOfficerData") {
          implicit val reads = LodgingOfficer.patchJsonReads
          withJsonBody[JsObject] { officer =>
            lodgingOfficerService.updateLodgingOfficerData(regId, officer) sendResult("updateLodgingOfficerData",regId)
          }
        }
      }
  }
}

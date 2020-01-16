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

import auth.{Authorisation, AuthorisationResource}
import common.exceptions.MissingRegDocument
import javax.inject.Inject
import models.api.LodgingOfficer
import play.api.libs.json.{JsBoolean, JsObject, JsValue}
import play.api.mvc.{Action, AnyContent}
import services.LodgingOfficerService
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.play.bootstrap.controller.BaseController
import scala.concurrent.ExecutionContext.Implicits.global

class LodgingOfficerControllerImpl @Inject()(val lodgingOfficerService: LodgingOfficerService,
                                             val authConnector: AuthConnector) extends LodgingOfficerController{
  val resourceConn: AuthorisationResource                              = lodgingOfficerService.registrationRepository
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

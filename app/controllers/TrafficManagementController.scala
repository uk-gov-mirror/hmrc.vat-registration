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
import javax.inject.{Inject, Singleton}
import models.api.RegistrationInformation
import play.api.libs.json.{JsSuccess, JsValue, Json}
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import services.TrafficManagementService
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.play.bootstrap.controller.BackendController

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class TrafficManagementController @Inject()(controllerComponents: ControllerComponents,
                                            trafficManagementService: TrafficManagementService,
                                            val authConnector: AuthConnector)
                                           (implicit ec: ExecutionContext)
  extends BackendController(controllerComponents) with Authorisation {

  override val resourceConn: AuthorisationResource = trafficManagementService.trafficManagementRepository

  def getRegistrationInformation: Action[AnyContent] = Action.async { implicit request =>
    isAuthenticated { internalId =>
      trafficManagementService.getRegistrationInformation(internalId) map {
        case Some(regInfo) =>
          Ok(Json.toJson(regInfo))
        case _ =>
          NotFound
      }
    }
  }

  def upsertRegistrationInformation: Action[JsValue] = Action.async(parse.json) { implicit request =>
    isAuthenticated { _ =>
      request.body.validate[RegistrationInformation] match {
        case JsSuccess(RegistrationInformation(internalId, registrationId, status, regStartDate, channel), _) =>
          if (regStartDate.isDefined) {
            Future.successful(UnprocessableEntity("Registration start date is not supported by this API"))
          }
          else {
            trafficManagementService.upsertRegistrationInformation(internalId, registrationId, status, channel)
              .map (regInfo => Ok(Json.toJson(regInfo)))
          }
        case _ =>
          Future.successful(BadRequest)
      }
    }
  }

}

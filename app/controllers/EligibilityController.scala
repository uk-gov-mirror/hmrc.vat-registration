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
import javax.inject.Inject
import models.api.Eligibility
import org.slf4j.LoggerFactory
import play.api.libs.json.{JsObject, JsValue}
import play.api.mvc.{Action, AnyContent}
import services.EligibilityService
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.play.bootstrap.controller.BaseController
import scala.concurrent.ExecutionContext.Implicits.global

class EligibilityControllerImpl @Inject()(val eligibilityService: EligibilityService,
                                          val authConnector: AuthConnector) extends EligibilityController {

  val resourceConn = eligibilityService.registrationRepository
}

trait EligibilityController extends BaseController with Authorisation {

  private val logger = LoggerFactory.getLogger(getClass)
  val eligibilityService: EligibilityService

  @deprecated("Use getEligibilityData instead", "SCRS-11579")
  def getEligibility(regId: String): Action[AnyContent] = Action.async {
    implicit request =>
      isAuthorised(regId) { authResult =>
        authResult.ifAuthorised(regId, "EligibilityController", "getEligibility") {
          eligibilityService.getEligibility(regId) sendResult("getEligibility", regId)
        }
      }
  }

  def getEligibilityData(regId: String): Action[AnyContent] = Action.async {
    implicit request =>
      isAuthorised(regId) { authResult =>
        authResult.ifAuthorised(regId, "EligibilityController", "getEligibilityData") {
          eligibilityService.getEligibilityData(regId) sendResult("getEligibilityData", regId)
        }
      }
  }

  @deprecated("Use updateEligibilityData instead", "SCRS-11579")
  def updateEligibility(regId: String): Action[JsValue] = Action.async[JsValue](parse.json) {
    implicit request =>
      isAuthorised(regId) { authResult =>
        authResult.ifAuthorised(regId, "EligibilityController", "updateEligibility") {
          withJsonBody[Eligibility] { eligibility =>
            eligibilityService.upsertEligibility(regId, eligibility) sendResult("updateEligibility",regId)
          }
        }
      }
  }

  def updateEligibilityData(regId: String): Action[JsValue] = Action.async[JsValue](parse.json) {
    implicit request =>
      isAuthorised(regId) { authResult =>
        authResult.ifAuthorised(regId, "EligibilityController", "updateEligibilityData") {
          withJsonBody[JsObject] { eligibilityData =>
            eligibilityService.updateEligibilityData(regId, eligibilityData) sendResult("updateEligibilityData", regId)
          }
        }
      }
  }
}

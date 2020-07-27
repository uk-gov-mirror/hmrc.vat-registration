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

import javax.inject.{Inject, Singleton}
import models.external.IncorpStatus
import play.api.Logger
import play.api.libs.json.{JsValue, Reads}
import play.api.mvc.{Action, ControllerComponents}
import services.SubmissionService

import scala.concurrent.ExecutionContext.Implicits.global
import uk.gov.hmrc.play.bootstrap.controller.BackendController

@Singleton
class ProcessIncorporationsController @Inject()(val submissionService: SubmissionService,
                                                    controllerComponents: ControllerComponents)
                                                    extends BackendController(controllerComponents) {


  def processIncorp: Action[JsValue] = Action.async[JsValue](parse.json) {
    implicit request =>
      implicit val reads: Reads[IncorpStatus] = IncorpStatus.reads
      withJsonBody[IncorpStatus] { incorp =>

        submissionService.submitTopUpVatRegistration(incorp) map {
          if (_) Ok else BadRequest
        } recover {
          case ex =>
            Logger.warn(s"TopUp Submission failed - ${ex.getMessage}")
            throw ex
        }
      }
  }
}

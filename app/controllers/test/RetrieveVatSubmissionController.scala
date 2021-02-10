/*
 * Copyright 2021 HM Revenue & Customs
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

package controllers.test

import play.api.Logging
import play.api.libs.json.{JsError, JsSuccess, JsValue}
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import services.submission.SubmissionPayloadBuilder
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class RetrieveVatSubmissionController @Inject()(cc: ControllerComponents,
                                                submissionPayloadBuilder: SubmissionPayloadBuilder
                                               )(implicit ec: ExecutionContext) extends BackendController(cc) with Logging {

  def retrieveSubmissionJson(regId: String): Action[AnyContent] = Action.async { implicit request =>
    submissionPayloadBuilder.buildSubmissionPayload(regId) map (Ok(_))
  }

}

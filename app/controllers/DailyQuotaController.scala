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

import javax.inject.Inject
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import services.DailyQuotaService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.controller.BackendController

import scala.concurrent.ExecutionContext

class DailyQuotaController @Inject()(controllerComponents: ControllerComponents,
                                     dailyQuotaService: DailyQuotaService
                                    )(implicit ec: ExecutionContext) extends BackendController(controllerComponents) {

  def canAllocate: Action[AnyContent] = Action.async { implicit request =>
    dailyQuotaService.canAllocate map {
      case true => NoContent
      case false => TooManyRequests
      case _ => throw new RuntimeException("[DailyQuotaController][canAllocate]] Unexpected error when checking daily quota")
    }
  }

  def updateDailyTotal: Action[AnyContent] = Action.async { implicit request =>
    dailyQuotaService.incrementTotal map (total => Ok(total.toString))
  }

}

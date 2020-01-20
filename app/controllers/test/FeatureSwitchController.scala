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

package controllers.test

import javax.inject.Inject

import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.play.bootstrap.controller.BaseController
import utils.{BooleanFeatureSwitch, FeatureSwitch, VATFeatureSwitches}

import scala.concurrent.Future

class FeatureSwitchController @Inject()() extends BaseController {

  val fs =  FeatureSwitch

  def switch(featureName: String, featureState: String): Action[AnyContent] = Action.async {
    implicit request =>

      def feature: FeatureSwitch = featureState match {
        case "true" => fs.enable(BooleanFeatureSwitch(featureName, enabled = true))
        case _ => fs.disable(BooleanFeatureSwitch(featureName, enabled = false))
      }

      VATFeatureSwitches(featureName) match {
        case Some(_) => Future.successful(Ok(feature.toString))
        case None => Future.successful(BadRequest)
      }
  }

  def show: Action[AnyContent] = Action.async {
    implicit request =>
      val f = VATFeatureSwitches.all.foldLeft("")((s: String, fs: FeatureSwitch) => s + s"""${fs.name} ${fs.enabled}\n""")
      Future.successful(Ok(f))
  }
}

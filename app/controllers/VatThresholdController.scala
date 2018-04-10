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

import java.io.FileInputStream

import javax.inject.Inject
import org.joda.time.DateTime
import org.joda.time.base.AbstractInstant._
import org.joda.time.format.DateTimeFormat
import play.api.libs.json.{JsArray, JsObject, JsValue, Json}
import play.api.mvc._
import uk.gov.hmrc.play.microservice.controller.BaseController
import services._

import util.control.Breaks._
import scala.concurrent.Future
import scala.util.parsing.json.JSONObject
import scala.util.{Failure, Success, Try}

class VatThresholdControllerImpl @Inject()(val vatThresholdService: VatThresholdService) extends VatThresholdController {}

trait VatThresholdController extends BaseController {
  val vatThresholdService: VatThresholdService

  def getThresholdForTime(): Action[JsValue] = Action.async(parse.json) {
    implicit request =>
      withJsonBody[JsObject] { json =>
        val inputDate = DateTimeFormat.forPattern("yyyy-MM-dd").parseDateTime((json \ "date").as[String])
        vatThresholdService.getThresholdForGivenTime(inputDate) match {
          case Some(vatThreshold) => Future.successful(Ok(Json.toJson(vatThreshold)))
          case _ => Future.successful(NotFound)
        }
      }
  }
}

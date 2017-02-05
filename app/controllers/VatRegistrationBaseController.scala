/*
 * Copyright 2017 HM Revenue & Customs
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

import auth.Authenticated
import common.exceptions.LeftState
import play.api.libs.json.{Json, Writes}
import play.api.mvc.Result
import services.ServiceResult
import uk.gov.hmrc.play.microservice.controller.BaseController

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

abstract class VatRegistrationBaseController extends BaseController with Authenticated {

  private[controllers] def handle[T](f: (T) => Result): (Either[LeftState, T]) => Result = {
    case Right(entity) => f(entity)
    case Left(NotFound) => Gone
    case _ => ServiceUnavailable
  }

  protected def patch[T: Writes](serviceCall: (String, T) => ServiceResult[T], regId: String): T => Future[Result] =
    (t: T) => serviceCall(regId, t).value.map(handle(u => Created(Json.toJson(u))))

}
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

import common.exceptions.MissingRegDocument
import play.api.libs.json.{Json, Writes}
import play.api.mvc.Result
import play.api.mvc.Results._

import scala.concurrent.{ExecutionContext, Future}

package object controllers {
  implicit class HandleResultToSend[T](f: Future[Option[T]])(implicit writes: Writes[T], ec: ExecutionContext) {
    def sendResult: Future[Result] = {
      f map {
        _.fold(NoContent)(data => Ok(Json.toJson(data)))
      } recover {
        case _: MissingRegDocument => NotFound
      }
    }
  }
}

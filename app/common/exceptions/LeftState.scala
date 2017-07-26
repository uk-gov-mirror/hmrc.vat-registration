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

package common.exceptions

import play.api.libs.json.Json
import play.api.mvc.Result
import play.api.mvc.Results._

sealed trait LeftState extends Product with Serializable {

  private def toResult(status: play.api.mvc.Results.Status, msg: String) =
    status(Json.obj("errorCode" -> status.header.status, "errorMessage" -> msg))

  def toResult: Result = this match {
    case ResourceNotFound(msg) => toResult(play.api.mvc.Results.NotFound, msg)
    case AcknowledgementReferenceExists(msg) => toResult(play.api.mvc.Results.Conflict, msg)
    case ForbiddenAccess(msg) => toResult(play.api.mvc.Results.Forbidden, msg)
    case GenericDatabaseError(t, regId) => toResult(ServiceUnavailable, s"Mongo exception: $t ; registration ID: ${regId.getOrElse("n/a")}")
    case GenericError(t) => toResult(ServiceUnavailable, s"Generic exception: $t")
    case NothingToReturn => play.api.mvc.Results.Ok("{}")
  }

}

final case class AcknowledgementReferenceExists(msg: String) extends LeftState

final case class ResourceNotFound(msg: String) extends LeftState

final case class ForbiddenAccess(msg: String) extends LeftState

final case class GenericDatabaseError(t: Throwable, regId: Option[String]) extends LeftState

final case class GenericError(t: Throwable) extends LeftState

case object NothingToReturn extends LeftState

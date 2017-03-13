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

import common.RegistrationId

import scala.util.control.NoStackTrace

sealed trait DBExceptions {
  val rid: RegistrationId
}

final case class PreExistingRegDocument(rid: RegistrationId) extends NoStackTrace with DBExceptions

final case class MissingRegDocument(rid: RegistrationId) extends NoStackTrace with DBExceptions

final case class UpdateFailed(rid: RegistrationId, attemptedModel: String) extends NoStackTrace with DBExceptions

final case class InsertFailed(rid: RegistrationId, attemptedModel: String) extends NoStackTrace with DBExceptions

final case class DeleteFailed(rid: RegistrationId, msg: String) extends NoStackTrace with DBExceptions

final case class RetrieveFailed(rid: RegistrationId) extends NoStackTrace with DBExceptions

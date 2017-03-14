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


import common.Identifiers.RegistrationId

import scala.util.control.NoStackTrace

sealed trait DBExceptions {
  val id: RegistrationId
}

final case class PreExistingRegDocument(id: RegistrationId) extends NoStackTrace with DBExceptions

final case class MissingRegDocument(id: RegistrationId) extends NoStackTrace with DBExceptions

final case class UpdateFailed(id: RegistrationId, attemptedModel: String) extends NoStackTrace with DBExceptions

final case class InsertFailed(id: RegistrationId, attemptedModel: String) extends NoStackTrace with DBExceptions

final case class DeleteFailed(id: RegistrationId, msg: String) extends NoStackTrace with DBExceptions

final case class RetrieveFailed(id: RegistrationId) extends NoStackTrace with DBExceptions

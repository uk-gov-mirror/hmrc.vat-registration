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

package deprecated

import models.api.Name

object DeprecatedConstants {
  @Deprecated
  val fakeNino = "AA123456A"

  @Deprecated
  val fakeOfficerName: Name = Name(first = Some("FAKE_FIRST_NAME"), middle = Some("FAKE_MIDDLE_NAME"), last = "[OFFICER_NAME_REMOVED]")
}

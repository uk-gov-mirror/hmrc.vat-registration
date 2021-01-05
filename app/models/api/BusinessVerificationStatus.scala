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

package models.api

import play.api.libs.json.{Format, JsResult, JsString, JsValue}


sealed trait BusinessVerificationStatus

case object BvPass extends BusinessVerificationStatus

case object BvFail extends BusinessVerificationStatus

case object BvUnchallenged extends BusinessVerificationStatus

object BusinessVerificationStatus {
  implicit val format: Format[BusinessVerificationStatus] = new Format[BusinessVerificationStatus] {
    val BvPassKey = "PASS"
    val BvFailKey = "FAIL"
    val BvUnchallengedKey = "UNCHALLENGED"

    override def writes(bvState: BusinessVerificationStatus): JsValue =
      bvState match {
        case BvPass => JsString(BvPassKey)
        case BvFail => JsString(BvFailKey)
        case BvUnchallenged => JsString(BvUnchallengedKey)
      }

    override def reads(json: JsValue): JsResult[BusinessVerificationStatus] =
      json.validate[String].map {
        case BvPassKey => BvPass
        case BvFailKey => BvFail
        case BvUnchallengedKey => BvUnchallenged
      }
  }

}

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

package models

import helpers.BaseSpec
import models.api.SicCode
import play.api.libs.json.{JsSuccess, Json}

class SicCodeSpec extends BaseSpec {

  val code1 = "code1"
  val code2 = "code2"
  val code3 = "code3"

  "Reading a list of SIC Codes" must {
    "return an empty list when no codes are provided" in {
      val res = SicCode.sicCodeListReads.reads(Json.obj())

      res mustBe JsSuccess(List())
    }
    "return a list with one code" in {
      val json = Json.obj(
        "mainCode2" -> code1
      )

      val res = SicCode.sicCodeListReads.reads(json)

      res mustBe JsSuccess(List(SicCode(code1, "", "")))
    }
    "return a list with two codes" in {
      val json = Json.obj(
        "mainCode2" -> code1,
        "mainCode3" -> code2
      )

      val res = SicCode.sicCodeListReads.reads(json)

      res mustBe JsSuccess(List(SicCode(code1, "", ""), SicCode(code2, "", "")))
    }
    "return a list with three codes" in {
      val json = Json.obj(
        "mainCode2" -> code1,
        "mainCode3" -> code2,
        "mainCode4" -> code3
      )

      val res = SicCode.sicCodeListReads.reads(json)

      res mustBe JsSuccess(List(SicCode(code1, "", ""), SicCode(code2, "", ""), SicCode(code3, "", "")))
    }
  }

  "Writing a list of SIC codes" must {
    "write empty JSON if no codes are provided" in {
      val res = SicCode.sicCodeListWrites.writes(List())

      res mustBe Json.obj()
    }
    "write one code" in {
      val res = SicCode.sicCodeListWrites.writes(List(SicCode(code1, "", "")))

      res mustBe Json.obj(
        "mainCode2" -> code1
      )
    }
    "write two codes" in {
      val res = SicCode.sicCodeListWrites.writes(
        List(SicCode(code1, "", ""), SicCode(code2, "", ""))
      )

      res mustBe Json.obj(
        "mainCode2" -> code1,
        "mainCode3" -> code2
      )
    }
    "write 3 codes" in {
      val res = SicCode.sicCodeListWrites.writes(
        List(SicCode(code1, "", ""), SicCode(code2, "", ""), SicCode(code3, "", ""))
      )

      res mustBe Json.obj(
        "mainCode2" -> code1,
        "mainCode3" -> code2,
        "mainCode4" -> code3
      )
    }
  }

}

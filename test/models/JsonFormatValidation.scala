/*
 * Copyright 2019 HM Revenue & Customs
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

import org.scalatest.Assertion
import play.api.data.validation.ValidationError
import play.api.libs.json.{JsError, JsPath, JsResult, JsSuccess}
import uk.gov.hmrc.play.test.UnitSpec

trait JsonFormatValidation extends UnitSpec {

  implicit class JsResultOps[T](res: JsResult[T]) {

    def resultsIn(t: T): Assertion = res match {
      case JsSuccess(deserialisedT, path) => deserialisedT shouldBe t
      case JsError(errors) => fail(s"found errors: $errors when expected: $t")
    }

    def shouldHaveErrors(expectedErrors: (JsPath, ValidationError)*): Unit = {
      val errorMap = Map(expectedErrors: _*)
      res match {
        case JsSuccess(t, _) => fail(s"read should have failed and didn't - produced $t")
        case JsError(errors) =>
          errors.size shouldBe errorMap.size
          for ((path, validationErrors) <- errors) {
            errorMap.keySet should contain(path)
            validationErrors should contain(errorMap(path))
          }
      }
    }
  }

}

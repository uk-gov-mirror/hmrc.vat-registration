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

package helpers

import akka.stream.{ActorMaterializer, Materializer}
import cats.data.{EitherT, OptionT}
import org.scalatest.Assertion
import org.scalatest.concurrent.ScalaFutures
import play.api.libs.json.JsValue
import play.api.mvc.Result
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.Future

trait FutureAssertions extends ScalaFutures {
  self: UnitSpec =>

  implicit class PlayFutureResultReturns(f: Future[Result]) {

    def returnsStatus(s: Int): Assertion = status(f) shouldBe s

    def returnsJson(j: JsValue)(implicit mat: Materializer): Assertion = whenReady(jsonBodyOf(f))(_ shouldBe j)

  }

  implicit class FutureReturns(f: Future[_]) {

    def returns(o: Any): Assertion = whenReady(f)(_ shouldBe o)

    def failedWith(e: Exception): Assertion = whenReady(f.failed)(_ shouldBe e)

    def failedWith[T <: Throwable](exClass: Class[T]): Assertion = whenReady(f.failed)(_.getClass shouldBe exClass)

  }

  implicit class OptionTReturns[T](ot: OptionT[Future, T]) {

    def returnsSome(t: T): Assertion = whenReady(ot.value)(_ shouldBe Some(t))

    def returnsNone: Assertion = whenReady(ot.value)(_ shouldBe Option.empty[T])

    def failedWith(e: Exception): Assertion = whenReady(ot.value.failed)(_ shouldBe e)

  }


  implicit class EitherTReturns[L, R](et: EitherT[Future, L, R]) {

    def returnsRight(value: R): Assertion = whenReady(et.value)(_ shouldBe Right(value))

    def returnsLeft(value: L): Assertion = whenReady(et.value)(_ shouldBe Left(value))

    def failedWith(e: Exception): Assertion = whenReady(et.value.failed)(_ shouldBe e)

  }

}

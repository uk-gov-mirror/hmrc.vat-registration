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

package itutil

import cats.data.{EitherT, OptionT}
import org.scalatest.concurrent.ScalaFutures
import play.api.mvc.Result
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.Future

trait FutureAssertions extends ScalaFutures {
  self: UnitSpec =>

  implicit class PlayFutureResultReturns(f: Future[Result]) {

    def returnsStatus(s: Int): Unit = status(f) shouldBe s

  }

  implicit class FutureReturns(f: Future[_]) {

    def returns(o: Any): Unit = whenReady(f)(_ shouldBe o)

    def failedWith(e: Exception): Unit = whenReady(f.failed)(_ shouldBe e)

    def failedWith[T <: Throwable](exClass: Class[T]): Unit = whenReady(f.failed)(_.getClass shouldBe exClass)

  }

  implicit class OptionTReturns[T](ot: OptionT[Future, T]) {

    def returnsSome(t: T): Unit = whenReady(ot.value)(_ shouldBe Some(t))

    def returnsNone: Unit = whenReady(ot.value)(_ shouldBe Option.empty[T])

    def failedWith(e: Exception): Unit = whenReady(ot.value.failed)(_ shouldBe e)

  }


  implicit class EitherTReturns[L, R](et: EitherT[Future, L, R]) {

    def returnsRight(value: R): Unit = whenReady(et.value)(_ shouldBe Right(value))

    def returnsLeft(value: L): Unit = whenReady(et.value)(_ shouldBe Left(value))

    def failedWith(e: Exception): Unit = whenReady(et.value.failed)(_ shouldBe e)

  }

}

/*
 * Copyright 2016 HM Revenue & Customs
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

package repository

import itutil.{ITFixtures, MongoBaseSpec}
import javax.inject.Inject
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import org.scalatest.{BeforeAndAfterAll, Matchers}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.test.Helpers._
import play.modules.reactivemongo.ReactiveMongoComponent
import repositories.SequenceMongoRepository
import uk.gov.hmrc.mongo.MongoSpecSupport

import scala.concurrent.ExecutionContext.Implicits.global

class SequenceMongoRepositoryISpec extends MongoBaseSpec with MongoSpecSupport with BeforeAndAfterAll
  with ScalaFutures with Eventually with GuiceOneAppPerSuite with ITFixtures {

  class Setup {
    val repository: SequenceMongoRepository = new SequenceMongoRepository(reactiveMongoComponent)

    await(repository.drop)
    await(repository.ensureIndexes)
  }

  override def afterAll(): Unit = new Setup {
    await(repository.drop)
  }

  val testSequence = "testSequence"

  "Sequence repository" should {
    "should be able to get a sequence ID" in new Setup {
      val response: Int = await(repository.getNext(testSequence))
      response mustBe 1
    }

    "get sequences, one after another from 1 to the end" in new Setup {
      val inputs: Seq[Int] = 1 to 25
      val outputs: Seq[Int] = inputs map { _ => await(repository.getNext(testSequence)) }
      outputs mustBe inputs
    }
  }
}
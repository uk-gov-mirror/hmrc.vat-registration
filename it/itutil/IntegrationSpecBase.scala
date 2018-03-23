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

import auth.Crypto
import models.api.VatScheme
import org.scalatest._
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatestplus.play.OneServerPerSuite
import play.api.Configuration
import play.api.libs.json.{JsString, Reads, Writes}
import play.api.libs.ws.WSClient
import play.modules.reactivemongo.ReactiveMongoComponent
import reactivemongo.api.commands.WriteResult
import repositories.{RegistrationMongo, RegistrationMongoRepository, SequenceMongo, SequenceMongoRepository}
import uk.gov.hmrc.crypto.{CompositeSymmetricCrypto, CryptoWithKeysFromConfig}
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait IntegrationSpecBase extends UnitSpec
  with OneServerPerSuite with ScalaFutures with IntegrationPatience with Matchers
  with WiremockHelper with BeforeAndAfterEach with BeforeAndAfterAll {

  val cryptoForTest: Crypto = new Crypto {
    def crypto: CompositeSymmetricCrypto = CryptoWithKeysFromConfig(
      baseConfigKey = "mongo-encryption",
      configuration = app.injector.instanceOf(classOf[Configuration]))
    override val rds: Reads[String] = Reads[String](_.validate[String])
    override val wts: Writes[String] = Writes[String](s => JsString(s))
  }

  trait SetupHelper {

    lazy val reactiveMongoComponent = app.injector.instanceOf[ReactiveMongoComponent]
    val mongo = new RegistrationMongo(reactiveMongoComponent, cryptoForTest)
    val sequenceMongo = new SequenceMongo(reactiveMongoComponent)
    val repo: RegistrationMongoRepository = mongo.store
    val sequenceRepository: SequenceMongoRepository = sequenceMongo.store

    await(repo.drop)
    await(repo.ensureIndexes)
    await(sequenceRepository.drop)
    await(sequenceRepository.ensureIndexes)

    def insertIntoDb(vatScheme: VatScheme): Future[WriteResult] = {
      val count =  await(repo.count)
      val res = await(repo.insert(vatScheme))
      await(repo.count) shouldBe count + 1
      res
    }

    lazy val ws   = app.injector.instanceOf(classOf[WSClient])
    def client(path: String) = ws.url(s"http://localhost:$port$path").withFollowRedirects(false)
}

  override def beforeEach(): Unit = {
    resetWiremock()
  }

  override def beforeAll(): Unit = {
    super.beforeAll()
    startWiremock()
  }

  override def afterAll(): Unit = {
    stopWiremock()
    super.afterAll()
  }
}
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

import auth.CryptoSCRS
import models.api.VatScheme
import org.scalatest._
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatestplus.play.OneServerPerSuite
import play.api.Configuration
import play.api.libs.json.{JsString, Reads, Writes}
import play.api.libs.ws.WSClient
import play.api.test.FakeApplication
import play.api.test.Helpers._
import play.modules.reactivemongo.ReactiveMongoComponent
import reactivemongo.api.commands.WriteResult
import repositories.{RegistrationMongo, RegistrationMongoRepository, SequenceMongo, SequenceMongoRepository}
import uk.gov.hmrc.crypto.{CompositeSymmetricCrypto, CryptoWithKeysFromConfig}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait IntegrationSpecBase extends WordSpec with OneServerPerSuite with ScalaFutures with IntegrationPatience with Matchers
  with WiremockHelper with BeforeAndAfterEach with BeforeAndAfterAll {

  val cryptoForTest: CryptoSCRS = new CryptoSCRS {
    def crypto: CompositeSymmetricCrypto = new CryptoWithKeysFromConfig(
      baseConfigKey = "json.encryption",
      config = app.injector.instanceOf(classOf[Configuration]).underlying)
    override val rds: Reads[String] = Reads[String](_.validate[String])
    override val wts: Writes[String] = Writes[String](s => JsString(s))
  }

  val mockHost: String = WiremockHelper.wiremockHost
  val mockPort: String = WiremockHelper.wiremockPort.toString

  val config: Map[String, String] = Map(
    "auditing.consumer.baseUri.host" -> mockHost,
    "auditing.consumer.baseUri.port" -> mockPort,
    "microservice.services.auth.host" -> mockHost,
    "microservice.services.auth.port" -> mockPort,
    "microservice.services.business-registration.host" -> mockHost,
    "microservice.services.business-registration.port" -> mockPort,
    "microservice.services.des-stub.host" -> mockHost,
    "microservice.services.des-stub.port" -> mockPort,
    "microservice.services.des-service.host" -> mockHost,
    "microservice.services.des-service.port" -> mockPort,
    "microservice.services.company-registration.host" -> mockHost,
    "microservice.services.company-registration.port" -> mockPort,
    "microservice.services.incorporation-information.host" -> mockHost,
    "microservice.services.incorporation-information.port" -> mockPort,
    "microservice.services.incorporation-information.uri" -> "/incorporation-information",
    "microservice.services.ThresholdsJsonLocation" -> "conf/thresholds.json",
    "microservice.services.vat-registration.host" -> mockHost,
    "microservice.services.vat-registration.port" -> mockPort,
    "mongo-encryption.key" -> "ABCDEFGHIJKLMNOPQRSTUV=="
  )

  override implicit lazy val app = FakeApplication(additionalConfiguration = config)

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

    def insertIntoDb(vatScheme: VatScheme): WriteResult = {
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

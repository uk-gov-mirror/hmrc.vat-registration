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
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import org.scalatestplus.play.PlaySpec
import play.api.{Application, Configuration}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsString, Reads, Writes}
import play.api.libs.ws.{WSClient, WSRequest}
import play.api.test.DefaultAwaitTimeout
import play.api.test.Helpers._
import play.modules.reactivemongo.ReactiveMongoComponent
import reactivemongo.api.commands.WriteResult
import repositories.{RegistrationMongoRepository, SequenceMongoRepository}

import scala.concurrent.ExecutionContext.Implicits.global

trait IntegrationSpecBase extends PlaySpec
  with GuiceOneServerPerSuite with ScalaFutures with IntegrationPatience
  with WiremockHelper with BeforeAndAfterEach with BeforeAndAfterAll with DefaultAwaitTimeout {

  lazy val cryptoForTest: CryptoSCRS = new CryptoSCRS(config = app.injector.instanceOf(classOf[Configuration])) {

    override val rds: Reads[String] = Reads[String](_.validate[String])
    override val wts: Writes[String] = Writes[String](s => JsString(s))
  }

  val mockUrl: String = WiremockHelper.url
  val mockHost: String = WiremockHelper.wiremockHost
  val mockPort: String = WiremockHelper.wiremockPort.toString

  val config: Map[String, String] = Map(
    "auditing.consumer.baseUri.host" -> mockHost,
    "auditing.consumer.baseUri.port" -> mockPort,
    "microservice.services.auth.host" -> mockHost,
    "microservice.services.auth.port" -> mockPort,
    "microservice.services.business-registration.host" -> mockHost,
    "microservice.services.business-registration.port" -> mockPort,
    "microservice.services.des.url" -> mockUrl,
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

  override implicit lazy val app: Application = new GuiceApplicationBuilder()
    .configure(config)
    .configure("application.router" -> "testOnlyDoNotUseInAppConf.Routes")
    .build()

  trait SetupHelper {

    lazy val reactiveMongoComponent: ReactiveMongoComponent = app.injector.instanceOf[ReactiveMongoComponent]
    val repo: RegistrationMongoRepository = new RegistrationMongoRepository(reactiveMongoComponent, cryptoForTest)
    val sequenceRepository: SequenceMongoRepository = new SequenceMongoRepository(reactiveMongoComponent)

    await(repo.drop)
    await(repo.ensureIndexes)
    await(sequenceRepository.drop)
    await(sequenceRepository.ensureIndexes)

    def insertIntoDb(vatScheme: VatScheme): WriteResult = {
      val count = await(repo.count)
      val res = await(repo.insert(vatScheme))
      await(repo.count) mustBe count + 1
      res
    }

    lazy val ws: WSClient = app.injector.instanceOf(classOf[WSClient])

    def client(path: String): WSRequest = ws.url(s"http://localhost:$port$path").withFollowRedirects(false)
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

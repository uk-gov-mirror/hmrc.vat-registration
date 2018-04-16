/*
 * Copyright 2018 HM Revenue & Customs
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

package controllers

import itutil.{ITFixtures, IntegrationStubbing, WiremockHelper}
import models.api._
import play.api.libs.json.{JsObject, Json}
import play.api.test.FakeApplication

import scala.concurrent.ExecutionContext.Implicits.global

class VatThresholdControllerISpec extends IntegrationStubbing {

  val mockHost = WiremockHelper.wiremockHost
  val mockPort = WiremockHelper.wiremockPort
  val mockUrl = s"http://$mockHost:$mockPort"

  override implicit lazy val app = FakeApplication(additionalConfiguration = Map(
      "auditing.consumer.baseUri.host" -> s"$mockHost",
      "auditing.consumer.baseUri.port" -> s"$mockPort",
      "microservice.services.auth.host" -> s"$mockHost",
      "microservice.services.auth.port" -> s"$mockPort",
      "microservice.services.des-stub.host" -> s"$mockHost",
      "microservice.services.des-stub.port" -> s"$mockPort",
      "microservice.services.company-registration.host" -> s"$mockHost",
      "microservice.services.company-registration.port" -> s"$mockPort",
      "microservice.services.incorporation-information.host" -> s"$mockHost",
      "microservice.services.incorporation-information.port" -> s"$mockPort",
      "microservice.services.incorporation-information.uri" -> "/incorporation-information",
      "microservice.services.ThresholdsJsonLocation" -> "conf/thresholds.json",
      "mongo-encryption.key" -> "ABCDEFGHIJKLMNOPQRSTUV=="
    )
  )

  class Setup extends SetupHelper()

  "VatThresholds" should {
    "return valid threhold amount and change date for given date" in new Setup {
      val response = await(client(controllers.routes.VatThresholdController.getThresholdForDate("2001-06-04").url).get())
      response.status shouldBe 200
      response.body shouldBe """{"since":"2001-04-01","taxable-threshold":"54000"}"""
    }
  }
}

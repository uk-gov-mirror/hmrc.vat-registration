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
package api

import com.github.tomakehurst.wiremock.stubbing.StubMapping
import itutil.{IntegrationSpecBase, WiremockHelper}
import play.api.http.Status._
import play.api.libs.json.Json
import play.api.libs.ws.WS
import play.api.test.FakeApplication

class VatRegistrationBasicISpec extends IntegrationSpecBase {

  val mockHost = WiremockHelper.wiremockHost
  val mockPort = WiremockHelper.wiremockPort
  val mockUrl = s"http://$mockHost:$mockPort"

  override implicit lazy val app = FakeApplication(additionalConfiguration = Map(
    "auditing.consumer.baseUri.host" -> s"$mockHost",
    "auditing.consumer.baseUri.port" -> s"$mockPort",
    "microservice.services.auth.host" -> s"$mockHost",
    "microservice.services.auth.port" -> s"$mockPort"
  ))

  private def client(path: String) = WS.url(s"http://localhost:$port$path").withFollowRedirects(false)


  "VAT Registration API - for initial / basic calls" should {

    def setupSimpleAuthMocks(): StubMapping = {
      stubPost("/write/audit", OK, """{"x":2}""")
      stubGet("/auth/authority", OK, """{"uri":"xxx","credentials":{"gatewayId":"xxx2"},"userDetailsLink":"xxx3","ids":"/auth/ids"}""")
      stubGet("/auth/ids", OK, """{"internalId":"Int-xxx","externalId":"Ext-xxx"}""")
    }

    "Return a 200 for " in {
      setupSimpleAuthMocks()

      val response = client(controllers.routes.HelloWorldController.hello().url).get.futureValue
      response.status shouldBe OK
      response.json shouldBe Json.parse("""{"uri":"xxx","gatewayId":"xxx2","userDetailsLink":"xxx3","ids":{"internalId":"Int-xxx","externalId":"Ext-xxx"}}""")
    }

    "Return a 403 for " in {
      val response = client(controllers.routes.HelloWorldController.hello().url).get.futureValue
      response.status shouldBe FORBIDDEN
    }

    "Return a 404 if the registration is missing" in {
      val response = client(s"/12345").get.futureValue
      response.status shouldBe NOT_FOUND
    }
  }
}
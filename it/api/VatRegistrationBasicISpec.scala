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
import common.RegistrationId
import itutil.{IntegrationStubbing, WiremockHelper}
import play.api.libs.json.Json
import play.api.libs.ws.WSClient
import play.api.test.FakeApplication
import play.api.test.Helpers._
import repositories.RegistrationMongoRepository
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global

class VatRegistrationBasicISpec extends IntegrationStubbing {

  implicit val hc: HeaderCarrier = HeaderCarrier()

  val mockHost = WiremockHelper.wiremockHost
  val mockPort = WiremockHelper.wiremockPort
  val mockUrl  = s"http://$mockHost:$mockPort"

  override implicit lazy val app = FakeApplication(additionalConfiguration = Map(
    "auditing.consumer.baseUri.host" -> s"$mockHost",
    "auditing.consumer.baseUri.port" -> s"$mockPort",
    "microservice.services.auth.host" -> s"$mockHost",
    "microservice.services.auth.port" -> s"$mockPort"
  ))

  lazy val repo = app.injector.instanceOf(classOf[RegistrationMongoRepository])
  lazy val ws   = app.injector.instanceOf(classOf[WSClient])

  private def client(path: String) = ws.url(s"http://localhost:$port$path").withFollowRedirects(false)

  "VAT Registration API - for initial / basic calls" should {

    "Return a 200 for " in {
      given
        .user.isAuthorised

      client(controllers.routes.VatRegistrationController.newVatRegistration().url).post("test") map { response =>
        response.status shouldBe OK
        response.json shouldBe Json.parse("""{"uri":"xxx","gatewayId":"xxx2","userDetailsLink":"xxx3","ids":{"internalId":"Int-xxx","externalId":"Ext-xxx"}}""")
      }
    }

    "Return a 403 for " in {
      client(controllers.routes.VatRegistrationController.newVatRegistration().url).post("test") map { response =>
        response.status shouldBe FORBIDDEN
      }
    }

    "Return a 404 if the registration is missing" in {
      client(s"/12345").post("test") map {
        _.status shouldBe NOT_FOUND
      }
    }
  }

  "/:regId/update-iv-status" should {
    "return an Ok" in {
      given
        .user.isAuthorised

      repo.createNewVatScheme(RegistrationId("testRegId"))

      val result = await(client(controllers.routes.VatRegistrationController.updateIVStatus("testRegId").url).patch(Json.parse("""{"ivPassed" : true}""")))
      result.status shouldBe OK

      await(repo.remove("registrationId" -> "testRegId"))
    }

    "return an INS" in {
      given
        .user.isAuthorised

      val result = await(client("/vatreg/testRegId2/update-iv-status").patch(Json.parse("""{"ivPassed" : true}""")))
      result.status shouldBe INTERNAL_SERVER_ERROR
    }
  }
}
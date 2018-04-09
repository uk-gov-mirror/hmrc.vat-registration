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

  override implicit lazy val app = FakeApplication(
    additionalConfiguration = Map(
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
      "mongo-encryption.key" -> "ABCDEFGHIJKLMNOPQRSTUV=="
    )
  )

  class Setup extends SetupHelper()

  val validBody = Json.parse(s"""
       |{"date":"2001-06-04"}
     """.stripMargin).as[JsObject]

  "VatThresholds" should {
    "return valid threhold amount and change date for given date" in new Setup {
      val response = await(
        client(
          controllers.routes.VatThresholdController.getThresholdForTime().url)
          .post(validBody))
      response.status shouldBe 200
      response.body shouldBe """{"since":"2001-04-01","taxable-threshold":"54000"}"""
    }
  }
}

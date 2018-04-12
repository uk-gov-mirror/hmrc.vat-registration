
package controllers

import java.time.LocalDate

import com.github.tomakehurst.wiremock.stubbing.StubMapping
import common.RegistrationId
import enums.VatRegStatus
import itutil.{ITFixtures, IntegrationStubbing, WiremockHelper}
import models.api.{Eligibility, Threshold, VatScheme}
import play.api.libs.json.{JsObject, Json}
import play.api.libs.ws.WSClient
import play.api.test.FakeApplication
import play.modules.reactivemongo.ReactiveMongoComponent
import reactivemongo.api.commands.WriteResult
import repositories.{RegistrationMongo, RegistrationMongoRepository, SequenceMongo, SequenceMongoRepository}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ThresholdControllerISpec extends IntegrationStubbing {

  val mockHost = WiremockHelper.wiremockHost
  val mockPort = WiremockHelper.wiremockPort
  val mockUrl  = s"http://$mockHost:$mockPort"

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
    "mongo-encryption.key" -> "ABCDEFGHIJKLMNOPQRSTUV=="
  ))

  class Setup extends SetupHelper {
    def writeAudit: StubMapping = stubPost("/write/audit/merged",200,"")
  }

  def vatScheme(regId: String): VatScheme = emptyVatScheme(regId).copy(threshold = Some(
    Threshold(
      false,
      Some("voluntaryReason"),
      Some(LocalDate.now()),
      Some(LocalDate.now()),
      Some(LocalDate.now()))
    )
  )

  val validThresholdJson = Json.parse(
    s"""
       |{
       | "mandatoryRegistration": false,
       | "voluntaryReason": "voluntaryReason",
       | "overThresholdDateThirtyDays": "${LocalDate.now()}",
       | "pastOverThresholdDateThirtyDays": "${LocalDate.now()}",
       | "overThresholdOccuredTwelveMonth": "${LocalDate.now()}"
       |}
    """.stripMargin).as[JsObject]

  val invalidThresholdJson = Json.parse(
    """
      |{
      | "mandatoryRegistration": "true-2"
      |}
    """.stripMargin).as[JsObject]

  "getThreshold" should {
    "return 200 if successfully obtained" in new Setup {
      given
        .user.isAuthorised

      insertIntoDb(vatScheme("regId"))

      await(client(controllers.routes.ThresholdController.getThreshold("regId").url).get() map { response =>
        response.status shouldBe 200
        response.json shouldBe validThresholdJson
      })
    }

    "return 204 if successfully obtained" in new Setup {
      given
        .user.isAuthorised

      insertIntoDb(emptyVatScheme("regId"))

      await(client(controllers.routes.ThresholdController.getThreshold("regId").url).get() map { response =>
        response.status shouldBe 204
      })
    }

    "return 404 if no document found" in new Setup {
      given
        .user.isAuthorised

      await(client(controllers.routes.ThresholdController.getThreshold("regId").url).get() map { response =>
        response.status shouldBe 404
      })
    }

    "return 403 if user is not authorised" in new Setup {
      given
        .user.isNotAuthorised

      await(client(controllers.routes.ThresholdController.getThreshold("regId").url).get() map { response =>
        response.status shouldBe 403
      })
    }
  }

  "updatingThreshold" should {
    "return 200 with a valid threshold json body" in new Setup {
      given
        .user.isAuthorised

      insertIntoDb(emptyVatScheme("regId"))

      await(client(controllers.routes.ThresholdController.updateThreshold("regId").url).patch(validThresholdJson) map { response =>
        response.status shouldBe 200
        response.json shouldBe validThresholdJson
      })
    }

    "return 400 if an invalid json body is posted" in new Setup {
      given
        .user.isAuthorised

      insertIntoDb(emptyVatScheme("regId"))

      await(client(controllers.routes.ThresholdController.updateThreshold("regId").url).patch(invalidThresholdJson) map { response =>
        response.status shouldBe 400
      })
    }

    "return 404 if no reg document is found" in new Setup {
      given
        .user.isAuthorised

      await(client(controllers.routes.ThresholdController.updateThreshold("regId").url).patch(validThresholdJson) map { response =>
        response.status shouldBe 404
      })
    }

    "return 200 if no data updated because data to be updated already exists" in new Setup {
      given
        .user.isAuthorised

      await(repo.insert(vatScheme))

      await(client(controllers.routes.ThresholdController.updateThreshold("regId").url).patch(validThresholdJson) map { response =>
        response.status shouldBe 200
      })
    }

    "return 403 if user is not authorised" in new Setup {
      given
        .user.isNotAuthorised

      await(client(controllers.routes.ThresholdController.updateThreshold("regId").url).patch(validThresholdJson) map { response =>
        response.status shouldBe 403
      })
    }
  }
}

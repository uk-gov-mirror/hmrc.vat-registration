
package controllers

import java.time.LocalDate

import com.github.tomakehurst.wiremock.stubbing.StubMapping
import common.RegistrationId
import enums.VatRegStatus
import itutil.{ITFixtures, IntegrationStubbing, WiremockHelper}
import models.api.{FRSDetails, FlatRateScheme, StartDate, VatScheme}
import play.api.libs.json.{JsObject, Json}
import play.api.libs.ws.WSClient
import play.api.test.FakeApplication
import play.modules.reactivemongo.ReactiveMongoComponent
import reactivemongo.api.commands.WriteResult
import repositories.{RegistrationMongo, RegistrationMongoRepository, SequenceMongo, SequenceMongoRepository}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class FlatRateSchemeControllerISpec extends IntegrationStubbing with ITFixtures {

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

  lazy val reactiveMongoComponent = app.injector.instanceOf[ReactiveMongoComponent]
  lazy val ws   = app.injector.instanceOf(classOf[WSClient])

  private def client(path: String) = ws.url(s"http://localhost:$port$path").withFollowRedirects(false)

  class Setup {
    val mongo = new RegistrationMongo(reactiveMongoComponent, cryptoForTest)
    val sequenceMongo = new SequenceMongo(reactiveMongoComponent)
    val repo: RegistrationMongoRepository = mongo.store
    val sequenceRepository: SequenceMongoRepository = sequenceMongo.store

    await(repo.drop)
    await(repo.ensureIndexes)
    await(sequenceRepository.drop)
    await(sequenceRepository.ensureIndexes)

    def insertIntoDb(vatScheme: VatScheme): Future[WriteResult] = await(repo.insert(vatScheme))
  }

  val dateNow: LocalDate = LocalDate.of(2018, 1, 1)

  def vatScheme(regId: String): VatScheme = emptyVatScheme(regId).copy(flatRateScheme = Some(
    FlatRateScheme(joinFrs = true, Some(FRSDetails(overBusinessGoods = false, Some(true), Some(12345678), Some(StartDate(Some(dateNow))), "testCategory", 15))))
  )

  def emptyVatScheme(regId: String): VatScheme = VatScheme(id = RegistrationId(regId),status = VatRegStatus.draft)

  val validFullFlatRateSchemeJson: JsObject = Json.parse(
    s"""
       |{
       |  "joinFrs":true,
       |  "frsDetails":{
       |    "overBusinessGoods":false,
       |    "overBusinessGoodsPercent":true,
       |    "vatInclusiveTurnover":12345678,
       |    "start":{
       |      "date":"$dateNow"
       |    },
       |    "categoryOfBusiness":"testCategory",
       |    "percent":15
       |  }
       |}
     """.stripMargin).as[JsObject]

  val invalidFlatRateSchemeJson: JsObject = Json.parse(
    s"""
       |{
       |
       |}
     """.stripMargin).as[JsObject]


  "fetchFlatRateScheme" should {
    "return 200 if successfully obtained" in new Setup {
      given
        .user.isAuthorised

      insertIntoDb(vatScheme("regId"))

      await(client(controllers.routes.FlatRateSchemeController.fetchFlatRateScheme("regId").url).get() map { response =>
        response.status shouldBe 200
        response.json shouldBe validFullFlatRateSchemeJson
      })
    }

    "return 204 if successfully obtained" in new Setup {
      given
        .user.isAuthorised

      insertIntoDb(emptyVatScheme("regId"))

      await(client(controllers.routes.FlatRateSchemeController.fetchFlatRateScheme("regId").url).get() map { response =>
        response.status shouldBe 204
      })
    }

    "return 404 if no document found" in new Setup {
      given
        .user.isAuthorised

      await(client(controllers.routes.FlatRateSchemeController.fetchFlatRateScheme("regId").url).get() map { response =>
        response.status shouldBe 404
      })
    }

    "return 403 if user is not authorised" in new Setup {
      given
        .user.isNotAuthorised

      await(client(controllers.routes.FlatRateSchemeController.fetchFlatRateScheme("regId").url).get() map { response =>
        response.status shouldBe 403
      })
    }
  }

  "updatingFlatRateScheme" should {
    "return 200 with a valid threshold json body" in new Setup {
      given
        .user.isAuthorised

      insertIntoDb(emptyVatScheme("regId"))

      await(client(controllers.routes.FlatRateSchemeController.updateFlatRateScheme("regId").url).patch(validFullFlatRateSchemeJson) map { response =>
        response.status shouldBe 200
        response.json shouldBe validFullFlatRateSchemeJson
      })
    }

    "return 400 if an invalid json body is posted" in new Setup {
      given
        .user.isAuthorised

      insertIntoDb(emptyVatScheme("regId"))

      await(client(controllers.routes.FlatRateSchemeController.updateFlatRateScheme("regId").url).patch(invalidFlatRateSchemeJson) map { response =>
        response.status shouldBe 400
      })
    }

    "return 404 if no reg document is found" in new Setup {
      given
        .user.isAuthorised

      await(client(controllers.routes.FlatRateSchemeController.updateFlatRateScheme("regId").url).patch(validFullFlatRateSchemeJson) map { response =>
        response.status shouldBe 404
      })
    }

    "return 200 if no data updated because data to be updated already exists" in new Setup {
      given
        .user.isAuthorised

      await(repo.insert(vatScheme))

      await(client(controllers.routes.FlatRateSchemeController.updateFlatRateScheme("regId").url).patch(validFullFlatRateSchemeJson) map { response =>
        response.status shouldBe 200
      })
    }

    "return 403 if user is not authorised" in new Setup {
      given
        .user.isNotAuthorised

      await(client(controllers.routes.FlatRateSchemeController.updateFlatRateScheme("regId").url).patch(validFullFlatRateSchemeJson) map { response =>
        response.status shouldBe 403
      })
    }
  }

  "removeFlatRateScheme" should {
    "return 200 if successful" in new Setup {
      given
        .user.isAuthorised

      insertIntoDb(vatScheme("regId"))

      await(client(controllers.routes.FlatRateSchemeController.removeFlatRateScheme("regId").url).delete() map { response =>
        response.status shouldBe 200
      })
    }

    "return 404 if no reg document is found" in new Setup {
      given
        .user.isAuthorised

      await(client(controllers.routes.FlatRateSchemeController.removeFlatRateScheme("regId").url).delete() map { response =>
        response.status shouldBe 404
      })
    }

    "return 403 if user is not authorised" in new Setup {
      given
        .user.isNotAuthorised

      await(client(controllers.routes.FlatRateSchemeController.removeFlatRateScheme("regId").url).delete() map { response =>
        response.status shouldBe 403
      })
    }
  }
}

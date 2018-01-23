package controllers

import common.RegistrationId
import enums.VatRegStatus
import itutil.{ITFixtures, IntegrationStubbing, WiremockHelper}
import models.api._
import play.api.libs.json.{JsObject, Json}
import play.api.libs.ws.WSClient
import play.api.test.FakeApplication
import play.modules.reactivemongo.ReactiveMongoComponent
import repositories.{RegistrationMongo, RegistrationMongoRepository}

import scala.concurrent.ExecutionContext.Implicits.global

class SicAndComplianceControllerISpec extends IntegrationStubbing with ITFixtures  {

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
    "microservice.services.incorporation-information.uri" -> "/incorporation-information"
  ))

  lazy val reactiveMongoComponent = app.injector.instanceOf[ReactiveMongoComponent]
  lazy val ws   = app.injector.instanceOf(classOf[WSClient])

  private def client(path: String) = ws.url(s"http://localhost:$port$path").withFollowRedirects(false)

  class Setup {
    val mongo = new RegistrationMongo(reactiveMongoComponent)
    val repo: RegistrationMongoRepository = mongo.store
    await(repo.drop)
    await(repo.ensureIndexes)
  }
    val validSicAndCompliance = Some(SicAndCompliance(
      "this is my business description",
      Some(ComplianceLabour(1000, Some(true), Some(true))),
      SicCode("12345678", "the flu", "sic details")
    ))

    val validSicAndComplianceJson = Json.parse(
      s"""
         |{
         | "businessDescription": "this is my business description",
         | "labourCompliance" : {
         | "numberOfWorkers": 1000,
         | "temporaryContracts":true,
         | "skilledWorkers":true
           },
       "mainBusinessActivity": {
       "id": "12345678",
       "description": "the flu",
       "displayDetails": "sic details"
           }
         |}
    """.stripMargin).as[JsObject]

  val validUpdatedSicAndCompliancejson = Json.parse(
    s"""
       |{
       | "businessDescription": "fooBar",
       | "labourCompliance" : {
       | "numberOfWorkers": 10,
       | "temporaryContracts":false
           },
       "mainBusinessActivity": {
       "id": "12345679",
       "description": "the flu 1",
       "displayDetails": "sic details 1"
           }
       |}
    """.stripMargin).as[JsObject]

  val invalidSicAndComplianceJson = Json.parse(
    s"""
       |{"fooBar":"fooWizzBarBang"
       |}
     """.stripMargin).as[JsObject]


  def vatScheme(regId: String): VatScheme = emptyVatScheme(regId).copy(sicAndCompliance = validSicAndCompliance)

  def emptyVatScheme(regId: String): VatScheme = VatScheme(id = RegistrationId(regId),status = VatRegStatus.draft)
    "getSicAndCompliance" should {
      "return 200" in new Setup {
        given
          .user.isAuthorised
          .regRepo.insertIntoDb(vatScheme("foo"), repo.insert)

        await(client(controllers.routes.SicAndComplianceController.getSicAndCompliance("foo").url).get() map { response =>
          response.status shouldBe 200
          response.json shouldBe validSicAndComplianceJson
        })
      }
      "return 204 when no SicAndComplianceRecord is found but reg doc exists" in new Setup {
        given
          .user.isAuthorised
            .regRepo.insertIntoDb(emptyVatScheme("foo"), repo.insert)

        await(client(controllers.routes.SicAndComplianceController.getSicAndCompliance("foo").url).get() map { response =>
          response.status shouldBe 204
        })
      }
      "return 404 when no reg doc is found" in new Setup {
        given
          .user.isAuthorised

        await(client(controllers.routes.SicAndComplianceController.getSicAndCompliance("fooBar").url).get() map { response =>
          response.status shouldBe 404
        })
      }
      "return 403 when user is not authorised" in new Setup {
        given
          .user.isNotAuthorised

        await(client(controllers.routes.SicAndComplianceController.getSicAndCompliance("fooBar").url).get() map { response =>
          response.status shouldBe 403
        })
      }
      }
  "updateSicAndCompliance" should {
    "return 200 during update to existing sicAndComp record" in new Setup {
      given
        .user.isAuthorised
        .regRepo.insertIntoDb(vatScheme("fooBar"),repo.insert)

      await(client(controllers.routes.SicAndComplianceController.updateSicAndCompliance("fooBar").url).patch(validUpdatedSicAndCompliancejson)) map { response =>
        response.status shouldBe 200
        response.json shouldBe validUpdatedSicAndCompliancejson

      }
    }
    "return 200 during update to vat doc whereby no sicAndCompliance existed before" in new Setup {
      given
        .user.isAuthorised
        .regRepo.insertIntoDb(emptyVatScheme("fooBar"),repo.insert)

      await(client(controllers.routes.SicAndComplianceController.updateSicAndCompliance("fooBar").url).patch(validSicAndComplianceJson)) map { response =>
        response.status shouldBe 200
        response.json shouldBe validSicAndCompliance
      }
    }
      "return 404 during update when no regDoc exists" in new Setup {
        given
          .user.isAuthorised

        await(client(controllers.routes.SicAndComplianceController.updateSicAndCompliance("fooBar").url).patch(validSicAndComplianceJson)) map { response =>
          response.status shouldBe 404
        }
      }
    "return 400 if the json is invalid" in new Setup {
      given
        .user.isAuthorised

      await(client(controllers.routes.SicAndComplianceController.updateSicAndCompliance("fooBar").url).patch(invalidSicAndComplianceJson)) map { response =>
        response.status shouldBe 400
      }
    }
    "return 403 when the user is not Authorised" in new Setup {
      given
        .user.isNotAuthorised

      await(client(controllers.routes.SicAndComplianceController.updateSicAndCompliance("fooBar").url).patch(validSicAndComplianceJson)) map { response =>
        response.status shouldBe 403
      }
    }
  }
  }



package controllers

import itutil.{IntegrationStubbing, WiremockHelper}
import models.api._
import play.api.libs.json.{JsObject, Json}
import play.api.test.FakeApplication

import scala.concurrent.ExecutionContext.Implicits.global

class SicAndComplianceControllerISpec extends IntegrationStubbing {

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
    "mongo-encryption.key" -> "ABCDEFGHIJKLMNOPQRSTUV=="
  ))
  val validSicAndCompliance = Some(SicAndCompliance(
    "this is my business description",
    Some(ComplianceLabour(1000, Some(true), Some(true))),
    SicCode("12345", "the flu", "sic details"),
    Nil
  ))
  val otherBusinessActivities =
    SicCode("00998", "otherBusiness desc 1", "fooBar 1") :: SicCode("00889", "otherBusiness desc 2", "fooBar 2") :: Nil
  val validSicAndComplianceWithOtherBusinessActivities =
    Some(validSicAndCompliance.get.copy(otherBusinessActivities = otherBusinessActivities))
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
       "code": "12345",
       "desc": "the flu",
       "indexes": "sic details"
           },
        "otherBusinessActivities":[]
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
       "code": "12345679",
       "desc": "the flu 1",
       "indexes": "sic details 1"
           },
       "otherBusinessActivities": [
       |{
       |       "code": "99889",
       |       "desc": "oherBusiness",
       |       "indexes": "otherBusiness1"
       |           }
       ]
       |}
    """.stripMargin).as[JsObject]
  val validSicAndComplianceJsonWithoutOtherBusinessActivities = Json.parse(
    s"""
       |{
       | "businessDescription": "this is my business description",
       | "labourCompliance" : {
       | "numberOfWorkers": 1000,
       | "temporaryContracts":true,
       | "skilledWorkers":true
           },
       "mainBusinessActivity": {
       "code": "12345",
       "desc": "the flu",
       "indexes": "sic details"
           }
       |}
    """.stripMargin).as[JsObject]
  val invalidSicAndComplianceJson = Json.parse(
    s"""
       |{"fooBar":"fooWizzBarBang"
       |}
     """.stripMargin).as[JsObject]

  def vatScheme(regId: String): VatScheme = emptyVatScheme(regId).copy(sicAndCompliance = validSicAndCompliance)

  class Setup extends SetupHelper

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
        .regRepo.insertIntoDb(vatScheme("fooBar"), repo.insert)

      await(client(controllers.routes.SicAndComplianceController.updateSicAndCompliance("fooBar").url).patch(validUpdatedSicAndCompliancejson)) map { response =>
        response.status shouldBe 200
        response.json shouldBe validUpdatedSicAndCompliancejson
      }
    }
    "return 200 during update to existing sicAndComp record whereby otherBusinessActivities existed but now do not and is not provided in the json" in new Setup {
      given
        .user.isAuthorised
        .regRepo.insertIntoDb(vatScheme("fooBar").copy(sicAndCompliance = validSicAndComplianceWithOtherBusinessActivities), repo.insert)

      await(client(controllers.routes.SicAndComplianceController.updateSicAndCompliance("fooBar").url).patch(validSicAndComplianceJsonWithoutOtherBusinessActivities)) map { response =>
        response.status shouldBe 200
        response.json shouldBe validSicAndComplianceJsonWithoutOtherBusinessActivities
      }

    }
    "return 200 during update to vat doc whereby no sicAndCompliance existed before" in new Setup {
      given
        .user.isAuthorised
        .regRepo.insertIntoDb(emptyVatScheme("fooBar"), repo.insert)

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



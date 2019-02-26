package controllers

import auth.CryptoSCRS
import itutil.{IntegrationStubbing, WiremockHelper}
import models.api.{Address, BusinessContact, DigitalContact, VatScheme}
import play.api.libs.json.{JsObject, Json}
import play.api.libs.ws.WSClient
import play.api.test.FakeApplication
import play.modules.reactivemongo.ReactiveMongoComponent
import repositories.{RegistrationMongo, RegistrationMongoRepository}

import scala.concurrent.ExecutionContext.Implicits.global

class BusinessContactControllerISpec extends IntegrationStubbing {

  val mockHost = WiremockHelper.wiremockHost
  val mockPort = WiremockHelper.wiremockPort
  val mockUrl  = s"http://$mockHost:$mockPort"

  override implicit lazy val app = FakeApplication(additionalConfiguration = Map(
    "auditing.consumer.baseUri.host" -> s"$mockHost",
    "auditing.consumer.baseUri.port" -> s"$mockPort",
    "microservice.services.auth.host" -> s"$mockHost",
    "microservice.services.auth.port" -> s"$mockPort",
    "microservice.services.company-registration.host" -> s"$mockHost",
    "microservice.services.company-registration.port" -> s"$mockPort",
    "microservice.services.incorporation-information.host" -> s"$mockHost",
    "microservice.services.incorporation-information.port" -> s"$mockPort",
    "microservice.services.incorporation-information.uri" -> "/incorporation-information",
    "mongo-encryption.key" -> "ABCDEFGHIJKLMNOPQRSTUV=="
  ))

  lazy val crypto: CryptoSCRS = app.injector.instanceOf(classOf[CryptoSCRS])

  class Setup extends SetupHelper

  val validBusinessContact  = Some(BusinessContact(
    digitalContact = DigitalContact("email@email.com",Some("12345"),Some("54321")),
    website = Some("www.foo.com"),
    ppob = Address("line1","line2",None,None,None,Some("foo"))
  ))

  val validBusinessContactJson = Json.parse(
    s"""{
       |"digitalContact":{
       |"email": "email@email.com",
       |"tel": "12345",
       |"mobile": "54321"
       |},
       |"website": "www.foo.com",
       |"ppob": {
       |"line1": "line1",
       |"line2": "line2",
       |"country": "foo"
       | }
       |}
       |
     """.stripMargin
  ).as[JsObject]

  val validUpdatedBusinessContactJson = Json.parse(
    s"""{
       |"digitalContact":{
       |"email": "email@email1.com",
       |"tel": "123456",
       |"mobile": "543210"
       |},
       |"website": "www.foobar.com",
       |"ppob": {
       |"line1": "line1a",
       |"line2": "line2b",
       |"country": "foobar"
       | }
       |}
       |
     """.stripMargin
  ).as[JsObject]


  def vatScheme(regId: String): VatScheme = emptyVatScheme(regId).copy(businessContact = validBusinessContact)

  "getBusinessContact" should {
    "return 200" in new Setup {
      given
        .user.isAuthorised
        .regRepo.insertIntoDb(vatScheme("foo"), repo.insert)

      await(client(controllers.routes.BusinessContactController.getBusinessContact("foo").url).get() map { response =>
        response.status shouldBe 200
        response.json shouldBe validBusinessContactJson
      })
    }
    "return 204 when no BusinessContact Record is found but reg doc exists" in new Setup {
      given
        .user.isAuthorised
        .regRepo.insertIntoDb(emptyVatScheme("foo"), repo.insert)

      await(client(controllers.routes.BusinessContactController.getBusinessContact("foo").url).get() map { response =>
        response.status shouldBe 204
      })
    }
    "return 404 when no reg doc is found" in new Setup {
      given
        .user.isAuthorised

      await(client(controllers.routes.BusinessContactController.getBusinessContact("fooBar").url).get() map { response =>
        response.status shouldBe 404
      })
    }
    "return 403 when user is not authorised" in new Setup {
      given
        .user.isNotAuthorised

      await(client(controllers.routes.BusinessContactController.getBusinessContact("fooBar").url).get() map { response =>
        response.status shouldBe 403
      })
    }
  }
  "updateBusinessConact" should {
    "return 200 during update to existing BusinessContact record" in new Setup {
      given
        .user.isAuthorised
        .regRepo.insertIntoDb(vatScheme("fooBar"),repo.insert)
      await(client(controllers.routes.BusinessContactController.updateBusinessContact("fooBar").url).patch(validUpdatedBusinessContactJson)) map { response =>
        response.status shouldBe 200
        response.json shouldBe validUpdatedBusinessContactJson

      }
    }
    "return 200 during update to vat doc whereby no BusinessContact existed before" in new Setup {
      given
        .user.isAuthorised
        .regRepo.insertIntoDb(emptyVatScheme("fooBar"),repo.insert)

      await(client(controllers.routes.BusinessContactController.updateBusinessContact("fooBar").url).patch(validBusinessContactJson)) map { response =>
        response.status shouldBe 200
        response.json shouldBe validBusinessContactJson
      }
    }
    "return 404 during update when no regDoc exists" in new Setup {
      given
        .user.isAuthorised

      await(client(controllers.routes.BusinessContactController.updateBusinessContact("fooBar").url).patch(validBusinessContactJson)) map { response =>
        response.status shouldBe 404
      }
    }
    "return 400 if the json is invalid" in new Setup {
      given
        .user.isAuthorised

      await(client(controllers.routes.BusinessContactController.updateBusinessContact("fooBar").url).patch(Json.obj("foo" -> Json.toJson("bar")))) map { response =>
        response.status shouldBe 400
      }
    }
    "return 403 when the user is not Authorised" in new Setup {
      given
        .user.isNotAuthorised

      await(client(controllers.routes.BusinessContactController.updateBusinessContact("fooBar").url).patch(validBusinessContactJson)) map { response =>
        response.status shouldBe 403
      }
    }
  }
}

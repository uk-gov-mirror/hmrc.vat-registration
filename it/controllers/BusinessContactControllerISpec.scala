
package controllers

import auth.CryptoSCRS
import itutil.IntegrationStubbing
import models.api.{Address, BusinessContact, DigitalContact, VatScheme}
import play.api.libs.json.{JsObject, Json}
import play.api.test.Helpers._
import controllers.routes.BusinessContactController

import scala.concurrent.ExecutionContext.Implicits.global

class BusinessContactControllerISpec extends IntegrationStubbing {

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
    "return OK" in new Setup {
      given.user.isAuthorised
        .regRepo.insertIntoDb(vatScheme("foo"), repo.insert)

      val response = await(client(BusinessContactController.getBusinessContact("foo").url).get())

      response.status shouldBe OK
      response.json shouldBe validBusinessContactJson
    }
    "return NO_CONTENT when no BusinessContact Record is found but reg doc exists" in new Setup {
      given.user.isAuthorised
        .regRepo.insertIntoDb(emptyVatScheme("foo"), repo.insert)

      val response = await(client(BusinessContactController.getBusinessContact("foo").url).get())

      response.status shouldBe NO_CONTENT
    }
    "return NOT_FOUND when no reg doc is found" in new Setup {
      given.user.isAuthorised

      val response = await(client(BusinessContactController.getBusinessContact("fooBar").url).get())

      response.status shouldBe NOT_FOUND
    }
    "return FORBIDDEN when user is not authorised" in new Setup {
      given.user.isNotAuthorised

      val response = await(client(BusinessContactController.getBusinessContact("fooBar").url).get())

      response.status shouldBe FORBIDDEN
    }
  }
  "updateBusinessConact" should {
    "return OK during update to existing BusinessContact record" in new Setup {
      given.user.isAuthorised
        .regRepo.insertIntoDb(vatScheme("fooBar"),repo.insert)

      val response = await(client(BusinessContactController.updateBusinessContact("fooBar").url)
          .patch(validUpdatedBusinessContactJson))

      response.status shouldBe OK
      response.json shouldBe validUpdatedBusinessContactJson
    }
    "return OK during update to vat doc whereby no BusinessContact existed before" in new Setup {
      given.user.isAuthorised
        .regRepo.insertIntoDb(emptyVatScheme("fooBar"),repo.insert)

      val response = await(client(BusinessContactController.updateBusinessContact("fooBar").url)
        .patch(validBusinessContactJson))

      response.status shouldBe OK
      response.json shouldBe validBusinessContactJson
    }
    "return NOT_FOUND during update when no regDoc exists" in new Setup {
      given.user.isAuthorised

      val response = await(client(BusinessContactController.updateBusinessContact("fooBar").url)
        .patch(validBusinessContactJson))

      response.status shouldBe NOT_FOUND
    }
    "return FORBIDDEN when the user is not Authorised" in new Setup {
      given.user.isNotAuthorised

      val response = await(client(BusinessContactController.updateBusinessContact("fooBar").url)
        .patch(validBusinessContactJson))

      response.status shouldBe FORBIDDEN
    }
  }
}

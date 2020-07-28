
package controllers

import com.github.tomakehurst.wiremock.stubbing.StubMapping
import itutil.IntegrationStubbing
import models.api.{Eligibility, VatScheme}
import play.api.libs.json.{JsObject, Json}
import play.api.test.Helpers._
import controllers.routes.EligibilityController
import play.api.libs.ws.WSResponse

class EligibilityControllerISpec extends IntegrationStubbing {

  class Setup extends SetupHelper {
    def writeAudit: StubMapping = stubPost("/write/audit/merged",OK,"")
  }

  def vatScheme(regId: String): VatScheme = emptyVatScheme(regId).copy(eligibility = Some(Eligibility(1,"success")))

  val validEligibilityJson: JsObject = Json.parse(
    """
      |{
      | "version": 1,
      | "result": "success"
      |}
    """.stripMargin).as[JsObject]

  val invalidEligibilityJson: JsObject = Json.parse(
    """
      |{
      | "result": "success"
      |}
    """.stripMargin).as[JsObject]

  "updatingEligibility" should {
    "return OK with an eligibility json body" in new Setup {
      given.user.isAuthorised

      insertIntoDb(emptyVatScheme("regId"))

      val response: WSResponse = await(client(EligibilityController.updateEligibility("regId").url)
        .patch(validEligibilityJson))

      response.status mustBe OK
      response.json mustBe validEligibilityJson
    }

    "return BAD_REQUEST if an invalid json body is posted" in new Setup {
      given.user.isAuthorised

      insertIntoDb(emptyVatScheme("regId"))

      val response: WSResponse = await(client(EligibilityController.updateEligibility("regId").url)
        .patch(invalidEligibilityJson))

      response.status mustBe BAD_REQUEST
    }

    "return NOT_FOUND if no reg document is found" in new Setup {
      given.user.isAuthorised

      val response: WSResponse = await(client(EligibilityController.updateEligibility("regId").url)
        .patch(validEligibilityJson))

      response.status mustBe NOT_FOUND
    }

    "return OK if no data updated because data is same" in new Setup {
      given.user.isAuthorised

      insertIntoDb(vatScheme("regId"))

      val response: WSResponse = await(client(EligibilityController.updateEligibility("regId").url)
        .patch(validEligibilityJson))

      response.status mustBe OK
    }

    "return FORBIDDEN if user is not authorised obtained" in new Setup {
      given.user.isNotAuthorised

      val response: WSResponse = await(client(EligibilityController.updateEligibility("regId").url)
        .patch(validEligibilityJson))

      response.status mustBe FORBIDDEN
    }
  }
}

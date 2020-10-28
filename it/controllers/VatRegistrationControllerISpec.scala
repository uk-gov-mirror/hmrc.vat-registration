
package controllers

import featureswitch.core.config.FeatureSwitching
import itutil.IntegrationStubbing
import play.api.libs.json.Json
import play.api.test.Helpers._

class VatRegistrationControllerISpec extends IntegrationStubbing with FeatureSwitching {

  class Setup extends SetupHelper

  "GET /new" should {
    "return CREATED if the daily quota has not been met" in new Setup {
      given
        .user.isAuthorised

      val res = await(client(controllers.routes.VatRegistrationController.newVatRegistration().url)
        .post(Json.obj())
      )

      res.status mustBe CREATED
    }
  }

}

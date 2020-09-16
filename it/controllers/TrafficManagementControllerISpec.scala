
package controllers

import java.time.LocalDate

import itutil.IntegrationStubbing
import models.api.{Draft, OTRS, RegistrationInformation, Submitted, VatReg}
import play.api.libs.json.Json

import scala.concurrent.ExecutionContext.Implicits.global
import play.api.test.Helpers._

class TrafficManagementControllerISpec extends IntegrationStubbing {

  class Setup extends SetupHelper

  val testRegId = "testRegId"

  val testRegInfo = RegistrationInformation(
    internalId = internalid,
    registrationId = testRegId,
    status = Draft,
    regStartDate = Some(LocalDate.parse(testDate)),
    channel = VatReg
  )

  "GET /traffic-management/reg-info" must {
    "return OK with reg info when a record exists for the internal ID" in new Setup {
      given
        .user.isAuthorised
        .regInfoRepo.insertIntoDb(testRegInfo, trafficManagementRepo.insert)

      val res = await(client(controllers.routes.TrafficManagementController.getRegistrationInformation.url).get)

      res.status mustBe OK
      res.json mustBe Json.toJson(testRegInfo)
    }

    "return NOT_FOUND when no record exists for the internal ID" in new Setup {
      given.user.isAuthorised

      val res = await(client(controllers.routes.TrafficManagementController.getRegistrationInformation.url).get)

      res.status mustBe NOT_FOUND
    }

    "return FORBIDDEN if the user is not authenticated" in new Setup {
      given.user.isNotAuthorised

      val res = await(client(controllers.routes.TrafficManagementController.getRegistrationInformation.url).get)

      res.status mustBe FORBIDDEN
    }
  }

  "PATCH /traffic-management/reg-info" must {
    "return OK with reg info when provided with valid JSON" in new Setup {
      given
        .user.isAuthorised
        .regInfoRepo.insertIntoDb(testRegInfo, trafficManagementRepo.insert)

      val updateData = RegistrationInformation(
        internalId = internalid,
        registrationId = testRegId,
        status = Submitted,
        channel = OTRS
      )

      val res = await(
        client(controllers.routes.TrafficManagementController.upsertRegistrationInformation().url)
          .patch(Json.toJson(updateData))
      )

      res.status mustBe OK
      res.body mustBe Json.toJson(testRegInfo.copy(status = Submitted, channel = OTRS)).toString()
    }

    "return BAD_REQUEST when provided with invalid JSON" in new Setup {
      given
        .user.isAuthorised
        .regInfoRepo.insertIntoDb(testRegInfo, trafficManagementRepo.insert)

      val updateData = Json.obj()

      val res = await(
        client(controllers.routes.TrafficManagementController.upsertRegistrationInformation().url)
          .patch(updateData)
      )

      res.status mustBe BAD_REQUEST
    }

    "return UNPROCESSABLE_ENTITY if the json contains a regStartDate" in new Setup {
      given
        .user.isAuthorised

      val res = await(
        client(controllers.routes.TrafficManagementController.upsertRegistrationInformation().url)
          .patch(Json.toJson(testRegInfo))
      )

      res.status mustBe UNPROCESSABLE_ENTITY
    }

    "return FORBIDDEN when the user is not authenticated" in new Setup {
      given
        .user.isNotAuthorised
        .regInfoRepo.insertIntoDb(testRegInfo, trafficManagementRepo.insert)

      val updateData = RegistrationInformation(
        internalId = internalid,
        registrationId = testRegId,
        status = Submitted,
        channel = OTRS
      )

      val res = await(
        client(controllers.routes.TrafficManagementController.upsertRegistrationInformation().url)
          .patch(Json.toJson(updateData))
      )

      res.status mustBe FORBIDDEN
    }
  }

}

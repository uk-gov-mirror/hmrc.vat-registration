/*
 * Copyright 2019 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package controllers

import java.time.LocalDate

import common.RegistrationId
import common.exceptions.MissingRegDocument
import fixtures.VatRegistrationFixture
import helpers.VatRegSpec
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.mockito.stubbing.OngoingStubbing
import play.api.libs.json.{JsBoolean, JsObject, Json}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.RegistrationMongoRepository

import scala.concurrent.Future

class LodgingOfficerControllerSpec extends VatRegSpec with VatRegistrationFixture {

  class Setup {
    val controller = new LodgingOfficerControllerImpl(lodgingOfficerService = mockLodgingOfficerService, authConnector = mockAuthConnector){
      override val resourceConn: RegistrationMongoRepository = mockRegistrationMongoRepository
    }

    def updateIVStatusSuccess(): OngoingStubbing[Future[Boolean]] = when(mockLodgingOfficerService.updateIVStatus(any(), any())(any()))
      .thenReturn(Future.successful(true))

    def updateIVStatusFails(): OngoingStubbing[Future[Boolean]] = when(mockLodgingOfficerService.updateIVStatus(any(), any())(any()))
      .thenReturn(Future.failed(new Exception))

    def updateIVStatusNotFound(): OngoingStubbing[Future[Boolean]] = when(mockLodgingOfficerService.updateIVStatus(any(), any())(any()))
      .thenReturn(Future.failed(MissingRegDocument(RegistrationId("testId"))))
  }

  val upsertLodgingOfficerJson = Json.parse(
    s"""
       |{
       | "name": {
       |   "first" : "Skylake",
       |   "last" : "Valiarm"
       | },
       | "dob" : "${LocalDate.now()}",
       | "nino" : "AB123456A",
       | "role" : "secretary",
       | "ivPassed" : true,
       | "details" : {
       |   "currentAddress" : {
       |     "line1" : "12 Lukewarm",
       |     "line2"  : "Oriental lane"
       |   },
       |   "contact" : {
       |     "email" : "skylake@vilikariet.com"
       |   }
       | },
       | "isOfficerApplying": true
       |}
    """.stripMargin).as[JsObject]

  val validLodgingOfficerJson = Json.parse(
    s"""
       |{
       | "name": {
       |   "first" : "Skylake",
       |   "last" : "Valiarm"
       | },
       | "dob" : "${LocalDate.now()}",
       | "nino" : "AB123456A",
       | "role" : "secretary",
       | "isOfficerApplying": true
       |}
    """.stripMargin).as[JsObject]

  val invalidLodgingOfficerJson = Json.parse(
    s"""
       |{
       | "dob" : "${LocalDate.now()}",
       | "nino" : "AB123456A",
       | "role" : "secretary"
       |}
    """.stripMargin).as[JsObject]

  "updateIVStatus" should {
    "returns 403 if user is not authorised" in new Setup {
      AuthorisationMocks.mockNotAuthorised(regId.value,internalid)

      val result = controller.updateIVStatus("testId", true)(FakeRequest())
      status(result) shouldBe 403
    }

    "returns 200 if successful" when {
      "the users IV status has been successfully updated" in new Setup {
        AuthorisationMocks.mockAuthorised(regId.value,internalid)
        updateIVStatusSuccess()

        val result = controller.updateIVStatus("testId", true)(FakeRequest())
        status(result) shouldBe 200
        await(contentAsJson(result)) shouldBe JsBoolean(true)
      }
    }

    "returns 404 if the registration is not found" in new Setup {
      AuthorisationMocks.mockAuthorised(regId.value,internalid)
      updateIVStatusNotFound()

      val result = controller.updateIVStatus("testId", true)(FakeRequest())
      status(result) shouldBe 404
    }

    "returns 500 if an error occurs" in new Setup {
      AuthorisationMocks.mockAuthorised(regId.value,internalid)
      updateIVStatusFails()

      val result = controller.updateIVStatus("testId", true)(FakeRequest())
      status(result) shouldBe 500
    }
  }

  "getLodgingOfficerData" should {
    "returns a valid json if found for id" in new Setup {
      AuthorisationMocks.mockAuthorised(regId.value,internalid)
      when(mockLodgingOfficerService.getLodgingOfficerData(any())(any()))
        .thenReturn(Future.successful(Some(validLodgingOfficerPreIV)))

      val result = controller.getLodgingOfficerData("testId")(FakeRequest())
      status(result) shouldBe 200
      await(contentAsJson(result)) shouldBe validLodgingOfficerJson
    }

    "returns 204 if none found" in new Setup {
      AuthorisationMocks.mockAuthorised(regId.value,internalid)
      when(mockLodgingOfficerService.getLodgingOfficerData(any())(any()))
        .thenReturn(Future.successful(None))

      val result = controller.getLodgingOfficerData("testId")(FakeRequest())

      status(result) shouldBe 204
    }

    "returns 404 if none found" in new Setup {
      AuthorisationMocks.mockAuthorised(regId.value,internalid)
      when(mockLodgingOfficerService.getLodgingOfficerData(any())(any()))
        .thenReturn(Future.failed(MissingRegDocument(RegistrationId("testId"))))

      val result = controller.getLodgingOfficerData("testId")(FakeRequest())
      status(result) shouldBe 404
    }

    "returns 403 if user is not authorised" in new Setup {
      AuthorisationMocks.mockNotAuthorised(regId.value,internalid)

      val result = controller.getLodgingOfficerData("testId")(FakeRequest())
      status(result) shouldBe 403
    }
  }

  "updateLodgingOfficerData" should {

    "returns 403 if user is not authorised" in new Setup {
      AuthorisationMocks.mockNotAuthorised(regId.value,internalid)

      val result = controller.updateLodgingOfficerData("testId")(FakeRequest().withBody[JsObject](upsertLodgingOfficerJson))
      status(result) shouldBe 403
    }

    "returns 200 if successful" in new Setup {
      AuthorisationMocks.mockAuthorised(regId.value,internalid)
      when(mockLodgingOfficerService.updateLodgingOfficerData(any(), any())(any()))
        .thenReturn(Future.successful(upsertLodgingOfficerJson))

      val result = controller.updateLodgingOfficerData("testId")(FakeRequest().withBody[JsObject](upsertLodgingOfficerJson))
      status(result) shouldBe 200
      await(contentAsJson(result)) shouldBe upsertLodgingOfficerJson
    }

    "returns 400 if json received is invalid" in new Setup {
      val invalidPatchLodgingOfficerJson = Json.parse(
        s"""
           |{
           | "nino" : "AB123456A",
           | "role" : "secretary"
           |}
    """.stripMargin).as[JsObject]

      AuthorisationMocks.mockAuthorised(regId.value,internalid)

      val result = controller.updateLodgingOfficerData("testId")(FakeRequest().withBody[JsObject](invalidPatchLodgingOfficerJson))
      status(result) shouldBe 400
    }

    "returns 404 if the registration is not found" in new Setup {
      AuthorisationMocks.mockAuthorised(regId.value,internalid)
      when(mockLodgingOfficerService.updateLodgingOfficerData(any(), any())(any()))
        .thenReturn(Future.failed(MissingRegDocument(RegistrationId("testId"))))

      val result = controller.updateLodgingOfficerData("testId")(FakeRequest().withBody[JsObject](upsertLodgingOfficerJson))
      status(result) shouldBe 404
    }

    "returns 500 if an error occurs" in new Setup {
      AuthorisationMocks.mockAuthorised(regId.value,internalid)
      when(mockLodgingOfficerService.updateLodgingOfficerData(any(), any())(any()))
        .thenReturn(Future.failed(new Exception))

      val result = controller.updateLodgingOfficerData("testId")(FakeRequest().withBody[JsObject](upsertLodgingOfficerJson))
      status(result) shouldBe 500
    }
  }
}
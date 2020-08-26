/*
 * Copyright 2020 HM Revenue & Customs
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
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.RegistrationMongoRepository

import scala.concurrent.Future

class LodgingOfficerControllerSpec extends VatRegSpec with VatRegistrationFixture {

  class Setup {
    val controller: LodgingOfficerController = new LodgingOfficerController(mockLodgingOfficerService, mockAuthConnector, stubControllerComponents()){
      override val resourceConn: RegistrationMongoRepository = mockRegistrationMongoRepository
    }

    def updateIVStatusSuccess(): OngoingStubbing[Future[Boolean]] = when(mockLodgingOfficerService.updateIVStatus(any(), any())(any()))
      .thenReturn(Future.successful(true))

    def updateIVStatusFails(): OngoingStubbing[Future[Boolean]] = when(mockLodgingOfficerService.updateIVStatus(any(), any())(any()))
      .thenReturn(Future.failed(new Exception))

    def updateIVStatusNotFound(): OngoingStubbing[Future[Boolean]] = when(mockLodgingOfficerService.updateIVStatus(any(), any())(any()))
      .thenReturn(Future.failed(MissingRegDocument(RegistrationId("testId"))))
  }

  val upsertLodgingOfficerJson: JsObject = Json.parse(
    s"""
       |{
       | "name": {
       |   "first" : "Skylake",
       |   "last" : "Valiarm"
       | },
       | "nino" : "AB123456A",
       | "role" : "secretary",
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

  val validLodgingOfficerJson: JsObject = Json.parse(
    s"""
       |{
       | "name": {
       |   "first" : "Skylake",
       |   "last" : "Valiarm"
       | },
       | "nino" : "AB123456A",
       | "role" : "secretary",
       | "isOfficerApplying": true
       |}
    """.stripMargin).as[JsObject]

  val invalidLodgingOfficerJson: JsObject = Json.parse(
    s"""
       |{
       | "nino" : "AB123456A",
       | "role" : "secretary"
       |}
    """.stripMargin).as[JsObject]

  "getLodgingOfficerData" should {
    "returns a valid json if found for id" in new Setup {
      AuthorisationMocks.mockAuthorised(regId.value,internalid)
      when(mockLodgingOfficerService.getLodgingOfficerData(any())(any()))
        .thenReturn(Future.successful(Some(validLodgingOfficerPreIV)))

      val result: Future[Result] = controller.getLodgingOfficerData("testId")(FakeRequest())
      status(result) mustBe 200
      contentAsJson(result) mustBe validLodgingOfficerJson
    }

    "returns 204 if none found" in new Setup {
      AuthorisationMocks.mockAuthorised(regId.value,internalid)
      when(mockLodgingOfficerService.getLodgingOfficerData(any())(any()))
        .thenReturn(Future.successful(None))

      val result: Future[Result] = controller.getLodgingOfficerData("testId")(FakeRequest())

      status(result) mustBe 204
    }

    "returns 404 if none found" in new Setup {
      AuthorisationMocks.mockAuthorised(regId.value,internalid)
      when(mockLodgingOfficerService.getLodgingOfficerData(any())(any()))
        .thenReturn(Future.failed(MissingRegDocument(RegistrationId("testId"))))

      val result: Future[Result] = controller.getLodgingOfficerData("testId")(FakeRequest())
      status(result) mustBe 404
    }

    "returns 403 if user is not authorised" in new Setup {
      AuthorisationMocks.mockNotAuthorised(regId.value,internalid)

      val result: Future[Result] = controller.getLodgingOfficerData("testId")(FakeRequest())
      status(result) mustBe 403
    }
  }

  "updateLodgingOfficerData" should {

    "returns 403 if user is not authorised" in new Setup {
      AuthorisationMocks.mockNotAuthorised(regId.value,internalid)

      val result: Future[Result] = controller.updateLodgingOfficerData("testId")(FakeRequest().withBody[JsObject](upsertLodgingOfficerJson))
      status(result) mustBe 403
    }

    "returns 200 if successful" in new Setup {
      AuthorisationMocks.mockAuthorised(regId.value,internalid)
      when(mockLodgingOfficerService.updateLodgingOfficerData(any(), any())(any()))
        .thenReturn(Future.successful(upsertLodgingOfficerJson))

      val result: Future[Result] = controller.updateLodgingOfficerData("testId")(FakeRequest().withBody[JsObject](upsertLodgingOfficerJson))
      status(result) mustBe 200
      contentAsJson(result) mustBe upsertLodgingOfficerJson
    }

    "returns 404 if the registration is not found" in new Setup {
      AuthorisationMocks.mockAuthorised(regId.value,internalid)
      when(mockLodgingOfficerService.updateLodgingOfficerData(any(), any())(any()))
        .thenReturn(Future.failed(MissingRegDocument(RegistrationId("testId"))))

      val result: Future[Result] = controller.updateLodgingOfficerData("testId")(FakeRequest().withBody[JsObject](upsertLodgingOfficerJson))
      status(result) mustBe 404
    }

    "returns 500 if an error occurs" in new Setup {
      AuthorisationMocks.mockAuthorised(regId.value,internalid)
      when(mockLodgingOfficerService.updateLodgingOfficerData(any(), any())(any()))
        .thenReturn(Future.failed(new Exception))

      val result: Future[Result] = controller.updateLodgingOfficerData("testId")(FakeRequest().withBody[JsObject](upsertLodgingOfficerJson))
      status(result) mustBe 500
    }
  }
}
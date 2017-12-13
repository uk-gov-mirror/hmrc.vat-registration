/*
 * Copyright 2017 HM Revenue & Customs
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
import models.api.{Eligibility, LodgingOfficer}
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.mockito.stubbing.OngoingStubbing
import play.api.libs.json.{JsBoolean, JsObject, JsValue, Json}
import play.api.test.FakeRequest

import scala.concurrent.Future

class LodgingOfficerControllerSpec extends VatRegSpec with VatRegistrationFixture {

  import fakeApplication.materializer

  class Setup {
    val controller = new LodgingOfficerControllerImpl(
      lodgingOfficerService = mockLodgingOfficerService,
      auth = mockAuthConnector
    )
    def userIsAuthorised(): Unit = AuthorisationMocks.mockSuccessfulAuthorisation(testAuthority(userId))
    def userIsNotAuthorised(): Unit = AuthorisationMocks.mockNotLoggedInOrAuthorised()

    def getLodgingOfficerData(): OngoingStubbing[Future[Option[LodgingOfficer]]] = when(mockLodgingOfficerService.getLodgingOfficer(any())(any()))
      .thenReturn(Future.successful(Some(validLodgingOfficerPreIV)))

    def getNoLodgingOfficerData(): OngoingStubbing[Future[Option[LodgingOfficer]]] = when(mockLodgingOfficerService.getLodgingOfficer(any())(any()))
      .thenReturn(Future.successful(None))

    def updateLodgingOfficerSuccess(): OngoingStubbing[Future[LodgingOfficer]] = when(mockLodgingOfficerService.updateLodgingOfficer(any(), any())(any()))
      .thenReturn(Future.successful(validLodgingOfficerPostIv))

    def updateLodgingOfficerFails(): OngoingStubbing[Future[LodgingOfficer]] = when(mockLodgingOfficerService.updateLodgingOfficer(any(), any())(any()))
      .thenReturn(Future.failed(new Exception))

    def updateLodgingOfficerNotFound(): OngoingStubbing[Future[LodgingOfficer]] = when(mockLodgingOfficerService.updateLodgingOfficer(any(), any())(any()))
      .thenReturn(Future.failed(MissingRegDocument(RegistrationId("testId"))))

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
       | }
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
       | "role" : "secretary"
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

  "get Lodging Officer" should {
    "returns a valid json if found for id" in new Setup {
      userIsAuthorised()
      getLodgingOfficerData()

      val result = controller.getLodgingOfficer("testId")(FakeRequest())

      status(result) shouldBe 200
      jsonBodyOf(await(result)) shouldBe validLodgingOfficerJson
    }

    "returns 404 if none found" in new Setup {
      userIsAuthorised()
      getNoLodgingOfficerData()

      val result = controller.getLodgingOfficer("testId")(FakeRequest())

      status(result) shouldBe 404
    }

    "returns 403 if user is not authorised" in new Setup {
      userIsNotAuthorised()

      val result = controller.getLodgingOfficer("testId")(FakeRequest())

      status(result) shouldBe 403
    }
  }

  "Update Lodging Officer" should {

    "returns 403 if user is not authorised" in new Setup {
      userIsNotAuthorised()
      val result = controller.updateLodgingOfficer("testId")(FakeRequest().withBody[JsObject](upsertLodgingOfficerJson))
      status(result) shouldBe 403
    }

    "returns 200 if successful" in new Setup {
      userIsAuthorised()
      updateLodgingOfficerSuccess()
      val result = controller.updateLodgingOfficer("testId")(FakeRequest().withBody[JsObject](upsertLodgingOfficerJson))
      status(result) shouldBe 200
      jsonBodyOf(await(result)) shouldBe upsertLodgingOfficerJson
    }

    "returns 400 if json received is invalid" in new Setup {
      userIsAuthorised()
      updateLodgingOfficerFails()
      val result = controller.updateLodgingOfficer("testId")(FakeRequest().withBody[JsObject](invalidLodgingOfficerJson))
      status(result) shouldBe 400
    }

    "returns 404 if the registration is not found" in new Setup {
      userIsAuthorised()
      updateLodgingOfficerNotFound()
      val result = controller.updateLodgingOfficer("testId")(FakeRequest().withBody[JsObject](upsertLodgingOfficerJson))
      status(result) shouldBe 404
    }

    "returns 500 if an error occurs" in new Setup {
      userIsAuthorised()
      updateLodgingOfficerFails()
      val result = controller.updateLodgingOfficer("testId")(FakeRequest().withBody[JsObject](upsertLodgingOfficerJson))
      status(result) shouldBe 500
    }
  }

  "updateIVStatus" should {
    "returns 403 if user is not authorised" in new Setup {
      userIsNotAuthorised()
      val result = controller.updateIVStatus("testId", true)(FakeRequest())
      status(result) shouldBe 403
    }

    "returns 200 if successful" when {
      "the users IV status has been successfully updated" in new Setup {
        userIsAuthorised()
        updateIVStatusSuccess()

        val result = controller.updateIVStatus("testId", true)(FakeRequest())
        status(result) shouldBe 200
        jsonBodyOf(await(result)) shouldBe JsBoolean(true)
      }
    }

    "returns 404 if the registration is not found" in new Setup {
      userIsAuthorised()
      updateIVStatusNotFound()
      val result = controller.updateIVStatus("testId", true)(FakeRequest())
      status(result) shouldBe 404
    }

    "returns 500 if an error occurs" in new Setup {
      userIsAuthorised()
      updateIVStatusFails()
      val result = controller.updateIVStatus("testId", true)(FakeRequest())
      status(result) shouldBe 500
    }
  }
}

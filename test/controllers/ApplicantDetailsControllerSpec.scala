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

import common.exceptions.MissingRegDocument
import fixtures.VatRegistrationFixture
import helpers.VatRegSpec
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.mockito.stubbing.OngoingStubbing
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.RegistrationMongoRepository

import scala.concurrent.Future

class ApplicantDetailsControllerSpec extends VatRegSpec with VatRegistrationFixture {

  class Setup {
    val controller: ApplicantDetailsController = new ApplicantDetailsController(mockApplicantDetailsService, mockAuthConnector, stubControllerComponents()){
      override val resourceConn: RegistrationMongoRepository = mockRegistrationMongoRepository
    }

    def updateIVStatusSuccess(): OngoingStubbing[Future[Boolean]] = when(mockApplicantDetailsService.updateIVStatus(any(), any())(any()))
      .thenReturn(Future.successful(true))

    def updateIVStatusFails(): OngoingStubbing[Future[Boolean]] = when(mockApplicantDetailsService.updateIVStatus(any(), any())(any()))
      .thenReturn(Future.failed(new Exception))

    def updateIVStatusNotFound(): OngoingStubbing[Future[Boolean]] = when(mockApplicantDetailsService.updateIVStatus(any(), any())(any()))
      .thenReturn(Future.failed(MissingRegDocument(regId)))
  }

  val upsertApplicantDetailsJson: JsObject = Json.parse(
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
       | "isApplicantApplying": true
       |}
    """.stripMargin).as[JsObject]

  val validApplicantDetailsJson: JsObject = Json.parse(
    s"""
       |{
       | "name": {
       |   "first" : "Skylake",
       |   "last" : "Valiarm"
       | },
       | "nino" : "AB123456A",
       | "role" : "secretary",
       | "isApplicantApplying": true
       |}
    """.stripMargin).as[JsObject]

  val invalidApplicantDetailsJson: JsObject = Json.parse(
    s"""
       |{
       | "nino" : "AB123456A",
       | "role" : "secretary"
       |}
    """.stripMargin).as[JsObject]

  "getApplicantDetailsData" should {
    "returns a valid json if found for id" in new Setup {
      AuthorisationMocks.mockAuthorised(regId,internalid)
      when(mockApplicantDetailsService.getApplicantDetailsData(any())(any()))
        .thenReturn(Future.successful(Some(validApplicantDetailsPreIV)))

      val result: Future[Result] = controller.getApplicantDetailsData(regId)(FakeRequest())
      status(result) mustBe 200
      contentAsJson(result) mustBe validApplicantDetailsJson
    }

    "returns 204 if none found" in new Setup {
      AuthorisationMocks.mockAuthorised(regId,internalid)
      when(mockApplicantDetailsService.getApplicantDetailsData(any())(any()))
        .thenReturn(Future.successful(None))

      val result: Future[Result] = controller.getApplicantDetailsData(regId)(FakeRequest())

      status(result) mustBe 204
    }

    "returns 404 if none found" in new Setup {
      AuthorisationMocks.mockAuthorised(regId,internalid)
      when(mockApplicantDetailsService.getApplicantDetailsData(any())(any()))
        .thenReturn(Future.failed(MissingRegDocument(regId)))

      val result: Future[Result] = controller.getApplicantDetailsData(regId)(FakeRequest())
      status(result) mustBe 404
    }

    "returns 403 if user is not authorised" in new Setup {
      AuthorisationMocks.mockNotAuthorised(regId,internalid)

      val result: Future[Result] = controller.getApplicantDetailsData(regId)(FakeRequest())
      status(result) mustBe 403
    }
  }

  "updateApplicantDetailsData" should {

    "returns 403 if user is not authorised" in new Setup {
      AuthorisationMocks.mockNotAuthorised(regId,internalid)

      val result: Future[Result] = controller.updateApplicantDetailsData(regId)(FakeRequest().withBody[JsObject](upsertApplicantDetailsJson))
      status(result) mustBe 403
    }

    "returns 200 if successful" in new Setup {
      AuthorisationMocks.mockAuthorised(regId,internalid)
      when(mockApplicantDetailsService.updateApplicantDetailsData(any(), any())(any()))
        .thenReturn(Future.successful(upsertApplicantDetailsJson))

      val result: Future[Result] = controller.updateApplicantDetailsData(regId)(FakeRequest().withBody[JsObject](upsertApplicantDetailsJson))
      status(result) mustBe 200
      contentAsJson(result) mustBe upsertApplicantDetailsJson
    }

    "returns 404 if the registration is not found" in new Setup {
      AuthorisationMocks.mockAuthorised(regId,internalid)
      when(mockApplicantDetailsService.updateApplicantDetailsData(any(), any())(any()))
        .thenReturn(Future.failed(MissingRegDocument(regId)))

      val result: Future[Result] = controller.updateApplicantDetailsData(regId)(FakeRequest().withBody[JsObject](upsertApplicantDetailsJson))
      status(result) mustBe 404
    }

    "returns 500 if an error occurs" in new Setup {
      AuthorisationMocks.mockAuthorised(regId,internalid)
      when(mockApplicantDetailsService.updateApplicantDetailsData(any(), any())(any()))
        .thenReturn(Future.failed(new Exception))

      val result: Future[Result] = controller.updateApplicantDetailsData(regId)(FakeRequest().withBody[JsObject](upsertApplicantDetailsJson))
      status(result) mustBe 500
    }
  }
}
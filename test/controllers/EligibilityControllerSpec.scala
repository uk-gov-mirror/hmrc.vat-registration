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
import models.api.Eligibility
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.mockito.stubbing.OngoingStubbing
import play.api.libs.json.{JsObject, JsValue, Json}
import play.api.test.FakeRequest
import repositories.RegistrationMongoRepository

import scala.concurrent.Future

class EligibilityControllerSpec extends VatRegSpec with VatRegistrationFixture {

  import play.api.test.Helpers._

  class Setup {
    val controller = new EligibilityControllerImpl (eligibilityService = mockEligibilityService, authConnector = mockAuthConnector){
      override val resourceConn: RegistrationMongoRepository = mockRegistrationMongoRepository
    }

    def getEligibilityData(): OngoingStubbing[Future[Option[Eligibility]]] = when(mockEligibilityService.getEligibility(any())(any()))
      .thenReturn(Future.successful(Some(validEligibility)))

    def getEligibilityDataNotFound(): OngoingStubbing[Future[Option[Eligibility]]] = when(mockEligibilityService.getEligibility(any())(any()))
      .thenReturn(Future.failed(MissingRegDocument(RegistrationId("testId"))))

    def getNoEligibilityData(): OngoingStubbing[Future[Option[Eligibility]]] = when(mockEligibilityService.getEligibility(any())(any()))
      .thenReturn(Future.successful(None))

    def updateEligibility(): OngoingStubbing[Future[Eligibility]] = when(mockEligibilityService.upsertEligibility(any(), any())(any()))
      .thenReturn(Future.successful(upsertEligibility))

    def updateEligibilityFails(): OngoingStubbing[Future[Eligibility]] = when(mockEligibilityService.upsertEligibility(any(), any())(any()))
      .thenReturn(Future.failed(new Exception))

    def updateEligibilityNotFound(): OngoingStubbing[Future[Eligibility]] = when(mockEligibilityService.upsertEligibility(any(), any())(any()))
      .thenReturn(Future.failed(new MissingRegDocument(RegistrationId("testId"))))
  }

  val validEligibilityJson = Json.parse(
    """
      |{
      | "version": 1,
      | "result": "thisIsAValidReason"
      |}
    """.stripMargin).as[JsObject]

  val upsertEligibilityJson = Json.parse(
    """
      |{
      | "version": 1,
      | "result": "thisIsAnUpsert"
      |}
    """.stripMargin).as[JsObject]

  val invalidUpsertJson = Json.parse(
    """
      |{
      | "result": "thisIsAnUpsert"
      |}
    """.stripMargin).as[JsObject]

  "getEligibility" should {
    "returns a valid json if found for id" in new Setup {
      AuthorisationMocks.mockAuthorised(regId.value,internalid)
      getEligibilityData()

      val result = controller.getEligibility("testId")(FakeRequest())
      status(result) shouldBe 200
      contentAsJson(result) shouldBe validEligibilityJson
    }

    "returns 204 if none found" in new Setup {
      AuthorisationMocks.mockAuthorised(regId.value,internalid)
      getNoEligibilityData()

      val result = controller.getEligibility("testId")(FakeRequest())
      status(result) shouldBe 204
    }

    "returns 404 if none found" in new Setup {
      AuthorisationMocks.mockAuthorised(regId.value,internalid)
      getEligibilityDataNotFound()

      val result = controller.getEligibility("testId")(FakeRequest())
      status(result) shouldBe 404
    }

    "returns 403 if user is not authorised" in new Setup {
      AuthorisationMocks.mockNotAuthorised(regId.value,internalid)

      val result = controller.getEligibility("testId")(FakeRequest())
      status(result) shouldBe 403
    }
  }

  "Upsert Eligibility" should {

    "returns 403 if user is not authorised" in new Setup {
      AuthorisationMocks.mockNotAuthorised(regId.value,internalid)

      val result = controller.updateEligibility("testId")(FakeRequest().withBody[JsObject](upsertEligibilityJson))
      status(result) shouldBe 403
    }

    "returns 200 if successful" in new Setup {
      AuthorisationMocks.mockAuthorised(regId.value,internalid)
      updateEligibility()

      val result = controller.updateEligibility("testId")(FakeRequest().withBody[JsObject](upsertEligibilityJson))
      status(result) shouldBe 200
      contentAsJson(result) shouldBe upsertEligibilityJson
    }

    "returns 400 if json received is invalid" in new Setup {
      AuthorisationMocks.mockAuthorised(regId.value,internalid)
      updateEligibilityFails()

      val result = controller.updateEligibility("testId")(FakeRequest().withBody[JsObject](invalidUpsertJson))
      status(result) shouldBe 400
    }

    "returns 404 if the registration is not found" in new Setup {
      AuthorisationMocks.mockAuthorised(regId.value,internalid)
      updateEligibilityNotFound()

      val result = controller.updateEligibility("testId")(FakeRequest().withBody[JsObject](upsertEligibilityJson))
      status(result) shouldBe 404
    }

    "returns 500 if an error occurs" in new Setup {
      AuthorisationMocks.mockAuthorised(regId.value,internalid)
      updateEligibilityFails()

      val result = controller.updateEligibility("testId")(FakeRequest().withBody[JsObject](upsertEligibilityJson))
      status(result) shouldBe 500
    }
  }
  "getEligibilityData" should {
    val json = Json.obj("foo" -> "bar")
    "return 200 and a JsObject" in new Setup {
      AuthorisationMocks.mockAuthorised(regId.value, internalid)
      when(mockEligibilityService.getEligibilityData(any())(any())).thenReturn(Future.successful(Some(json)))

      val res = controller.getEligibilityData(regId.value)(FakeRequest())
      status(res) shouldBe 200
      contentAsJson(res) shouldBe json
    }
    "return 204 when nothing exists" in new Setup {
      AuthorisationMocks.mockAuthorised(regId.value, internalid)
      when(mockEligibilityService.getEligibilityData(any())(any())).thenReturn(Future.successful(None))

      val res = controller.getEligibilityData(regId.value)(FakeRequest())
      status(res) shouldBe 204
    }
    "return 404 when no reg doc exists" in new Setup {
      AuthorisationMocks.mockAuthorised(regId.value, internalid)
      when(mockEligibilityService.getEligibilityData(any())(any())).thenReturn(Future.failed(new MissingRegDocument(RegistrationId("foo"))))

      val res = controller.getEligibilityData(regId.value)(FakeRequest())
      status(res) shouldBe 404
    }
  }
  "updateEligibilityData" should {
    val thresholdPreviousThirtyDays = LocalDate.of(2017, 5, 23)
    val thresholdInTwelveMonths = LocalDate.of(2017, 7, 16)
    val officer = Json.obj("role" -> "director", "name" -> Json.obj(
      "forename" -> "First Name Test",
      "other_forenames" -> "Middle Name Test",
      "surname" -> "Last Name Test"
    ))
    val json = Json.parse(
      s"""{
         |"sections": [
         |   {
         |     "title": "VAT details",
         |     "data": [
         |       {"questionId": "mandatoryRegistration", "question": "Some Question 11", "answer": "Some Answer 11", "answerValue": true},
         |       {"questionId": "voluntaryRegistration", "question": "Some Question 12", "answer": "Some Answer 12", "answerValue": false},
         |       {"questionId": "thresholdPreviousThirtyDays", "question": "Some Question 12", "answer": "Some Answer 12", "answerValue": "$thresholdPreviousThirtyDays"},
         |       {"questionId": "thresholdInTwelveMonths", "question": "Some Question 12", "answer": "Some Answer 12", "answerValue": "$thresholdInTwelveMonths"}
         |     ]
         |   },
         |   {
         |     "title": "Director details",
         |     "data": [
         |       {"questionId": "applicantUKNino", "question": "Some Question 11", "answer": "Some Answer 11", "answerValue": "SR123456C"},
         |       {"questionId": "turnoverEstimate", "question": "Some Question 11", "answer": "Some Answer 11", "answerValue": 2024},
         |       {"questionId": "completionCapacity", "question": "Some Question 11", "answer": "Some Answer 11", "answerValue": $officer},
         |       {"questionId":"fooDirectorDetails3","question": "Date of birth", "answer": "1 January 2000", "answerValue": true}
         |     ]
         |   }
         | ]
         |}
        """.stripMargin)

    "return 200 after a successful update" in new Setup {
      AuthorisationMocks.mockAuthorised(regId.value, internalid)
      when(mockEligibilityService.updateEligibilityData(any(), any())(any())).thenReturn(Future.successful(json.as[JsObject]))
      val result = controller.updateEligibilityData("testId")(FakeRequest().withBody[JsValue](json))
      status(result) shouldBe 200
    }
    "returns 403 if user is not authorised" in new Setup {
      AuthorisationMocks.mockNotAuthorised(regId.value,internalid)

      val result = controller.updateEligibilityData("testId")(FakeRequest().withBody[JsValue](json))
      status(result) shouldBe 403
    }

    "returns 400 if json received is invalid" in new Setup {
      val invalidJson = Json.parse(
        s"""
           |[
           |   {
           |     "title": "VAT details",
           |     "data": [
           |       {"questionId": "mandatoryRegistration", "question": "Some Question 11", "answer": "Some Answer 11", "answerValue": true},
           |       {"questionId": "voluntaryRegistration", "question": "Some Question 12", "answer": "Some Answer 12", "answerValue": false}
           |     ]
           |   },
           |   {
           |     "title": "Director details",
           |     "data": [
           |       {"questionId": "turnoverEstimate", "question": "Some Question 11", "answer": "Some Answer 11", "answerValue": 2024},
           |       {"questionId":"fooDirectorDetails3","question": "Date of birth", "answer": "1 January 2000", "answerValue": true}
           |     ]
           |   }
           |]
        """.stripMargin)
      AuthorisationMocks.mockAuthorised(regId.value,internalid)

      val result = controller.updateEligibilityData("testId")(FakeRequest().withBody[JsValue](invalidJson))
      status(result) shouldBe 400
    }

    "returns 404 if the registration is not found" in new Setup {
      AuthorisationMocks.mockAuthorised(regId.value,internalid)
      when(mockEligibilityService.updateEligibilityData(any(), any())(any()))
        .thenReturn(Future.failed(new MissingRegDocument(RegistrationId("testId"))))

      val result = controller.updateEligibilityData("testId")(FakeRequest().withBody[JsValue](json))
      status(result) shouldBe 404
    }

    "returns 500 if an error occurs" in new Setup {
      AuthorisationMocks.mockAuthorised(regId.value,internalid)
      when(mockEligibilityService.updateEligibilityData(any(), any())(any()))
        .thenReturn(Future.failed(new Exception))

      val result = controller.updateEligibilityData("testId")(FakeRequest().withBody[JsValue](json))
      status(result) shouldBe 500
    }
  }
}
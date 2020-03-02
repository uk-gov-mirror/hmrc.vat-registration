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

package services

import fixtures.VatRegistrationFixture
import helpers.VatRegSpec
import models.api.Eligibility
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.mockito.stubbing.OngoingStubbing
import play.api.libs.json.{JsArray, JsResultException, Json}
import play.api.test.Helpers._
import repositories.RegistrationMongoRepository

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class EligibilityServiceSpec extends VatRegSpec with VatRegistrationFixture {

  class Setup {
    val service = new EligibilityService(
      registrationMongo = mockRegistrationMongo
    ) {
      override val registrationRepository: RegistrationMongoRepository = mockRegistrationMongoRepository
    }

    def upsertToMongo(): OngoingStubbing[Future[Eligibility]] = when(mockRegistrationMongoRepository.updateEligibility(any(), any())(any()))
      .thenReturn(Future.successful(upsertEligibility))

    def upsertToMongoFail(): OngoingStubbing[Future[Eligibility]] = when(mockRegistrationMongoRepository.updateEligibility(any(), any())(any()))
      .thenReturn(Future.failed(new Exception("")))

    def getsFromMongo(): OngoingStubbing[Future[Option[Eligibility]]] = when(mockRegistrationMongoRepository.getEligibility(any())(any()))
      .thenReturn(Future.successful(Some(validEligibility)))

    def getsNothingFromMongo(): OngoingStubbing[Future[Option[Eligibility]]] = when(mockRegistrationMongoRepository.getEligibility(any())(any()))
      .thenReturn(Future.successful(None))
  }

  val upsertEligibilityModel = Json.parse(
    """
      |{
      | "version": 1,
      | "result": "thisIsAnUpsert"
      |}
    """.stripMargin).as[Eligibility]

  val validEligibilityModel = Json.parse(
    """
      |{
      | "version": 1,
      | "result": "thisIsAValidReason"
      |}
    """.stripMargin).as[Eligibility]

  "upsertEligibility" should {
    "return the data that is being inputted" in new Setup {
      upsertToMongo()
      val result = await(service.upsertEligibility("regId", upsertEligibilityModel))
      result shouldBe upsertEligibilityModel
    }

    "encounter an exception if an error occurs" in new Setup {
      upsertToMongoFail()
      intercept[Exception](await(service.upsertEligibility("regId", upsertEligibilityModel)))
    }
  }
  "getEligibility" should {
    "return an eligibility if found" in new Setup {
      getsFromMongo()
      val result = await(service.getEligibility("regId"))
      result shouldBe Some(validEligibilityModel)
    }

    "return None if none found matching regId" in new Setup {
      getsNothingFromMongo()
      val result = await(service.getEligibility("regId"))
      result shouldBe None
    }
  }


  val json = Json.obj("test" -> "value test")

  "getEligibilityData" should {
    "return an eligibility data json if found" in new Setup {
      when(mockRegistrationMongoRepository.getEligibilityData(any())(any()))
        .thenReturn(Future.successful(Some(json)))

      val result = await(service.getEligibilityData("regId"))
      result shouldBe Some(json)
    }

    "return None if none found matching regId" in new Setup {
      when(mockRegistrationMongoRepository.getEligibilityData(any())(any()))
        .thenReturn(Future.successful(None))

      val result = await(service.getEligibilityData("regId"))
      result shouldBe None
    }
  }

  "updateEligibilityData" should {
    val completionCapacity = Json.obj("role" -> "director", "name" -> Json.obj(
      "forename" -> "First Name Test",
      "other_forenames" -> "Middle Name Test",
      "surname" -> "Last Name Test"
    ))
    val questions1 = Seq(
      Json.obj("questionId" -> "completionCapacity", "question" -> "Some Question 11", "answer" -> "Some Answer 11", "answerValue" -> completionCapacity),
      Json.obj("questionId" -> "testQId12", "question" -> "Some Question 12", "answer" -> "Some Answer 12", "answerValue" -> "val12")
    )
    val questions2 = Seq(
      Json.obj("questionId" -> "applicantUKNino-optionalData", "question" -> "Some Question 22", "answer" -> "Some Answer 22", "answerValue" -> "JW778877A"),
      Json.obj("questionId" -> "turnoverEstimate-value", "question" -> "Some Question 21", "answer" -> "Some Answer 21", "answerValue" -> "oneandtenthousand"),
      Json.obj("questionId" -> "testQId22", "question" -> "Some Question 22", "answer" -> "Some Answer 22", "answerValue" -> "val22")
    )
    val section1 = Json.obj("title" -> "test TITLE 1", "data" -> JsArray(questions1))
    val section2 = Json.obj("title" -> "test TITLE 2", "data" -> JsArray(questions2))
    val sections = JsArray(Seq(section1, section2))
    val eligibilityData = Json.obj("sections" -> sections)

    "return the data that is being provided" in new Setup {
      when(mockRegistrationMongoRepository.updateEligibilityData(any(), any())(any()))
        .thenReturn(Future.successful(eligibilityData))

      val result = await(service.updateEligibilityData("regId", eligibilityData))
      result shouldBe eligibilityData
    }

    "encounter a JsResultException if json provided is incorrect" in new Setup {
      val incorrectQuestionValue = Json.obj("sections" -> JsArray(Seq(Json.obj("title" -> "test TITLE 1", "data" -> JsArray(Seq(Json.obj(
        "questionId" -> "thresholdPreviousThirtyDays-optionalData", "question" -> "Some Question 12", "answer" -> "Some Answer 12", "answerValue" -> "234324"
      )))))))
      val incorrectEligibilityData = eligibilityData.deepMerge(incorrectQuestionValue)
      intercept[JsResultException](await(service.updateEligibilityData("regId", incorrectEligibilityData)))
    }

    "encounter an exception if an error occurs" in new Setup {
      when(mockRegistrationMongoRepository.updateEligibilityData(any(), any())(any()))
        .thenReturn(Future.failed(new Exception("")))

      intercept[Exception](await(service.updateEligibilityData("regId", eligibilityData)))
    }
  }
}

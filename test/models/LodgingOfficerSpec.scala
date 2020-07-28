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

package models

import java.time.LocalDate

import fixtures.VatRegistrationFixture
import helpers.BaseSpec
import models.api._
import play.api.libs.json.{Format, JsArray, JsObject, JsPath, JsSuccess, Json, JsonValidationError, OFormat}
import utils.EligibilityDataJsonUtils

class LodgingOfficerSpec extends BaseSpec with JsonFormatValidation with VatRegistrationFixture {

  private def writeAndRead[T](t: T)(implicit fmt: Format[T]) = fmt.reads(Json.toJson(fmt.writes(t)))

  implicit val format: OFormat[LodgingOfficer] = LodgingOfficer.format

  val vatLodgingOfficer: LodgingOfficer = LodgingOfficer(
    dob                      = Some(LocalDate.of(1990, 1, 1)),
    nino                     = "NB686868C",
    role                     = "director",
    name                     = name,
    ivPassed                 = None,
    details                  = None
  )

  "Creating a Json from a valid VatLodgingOfficer model" should {
    "complete successfully" in {
      writeAndRead(vatLodgingOfficer) resultsIn vatLodgingOfficer
    }
  }

  "Creating a Json from an invalid VatLodgingOfficer model" should {
    "fail with a JsonValidationError" when {
      "NINO is invalid" in {
        val lodgingOfficer = vatLodgingOfficer.copy(nino = "NB888")
        writeAndRead(lodgingOfficer) shouldHaveErrors (JsPath() \ "nino" -> JsonValidationError("error.pattern"))
      }

      "Role is invalid" in {
        val lodgingOfficer = vatLodgingOfficer.copy(role = "magician")
        writeAndRead(lodgingOfficer) shouldHaveErrors (JsPath() \ "role" -> JsonValidationError("error.pattern"))
      }

      "Name is invalid" in {
        val name = Name(first = Some("$%@$%^@#%@$^@$^$%@#$%@#$"), middle = None, last = "valid name")
        val lodgingOfficer = vatLodgingOfficer.copy(name = name)
        writeAndRead(lodgingOfficer) shouldHaveErrors (JsPath() \ "name" \ "first" -> JsonValidationError("error.pattern"))
      }
    }
  }

  "mongoReads" should {
    "return LodgingOfficer model successfully" when {
      val lodgingOfficerJson = Json.parse(
        s"""
           |{
           |   "lodgingOfficer" : {
           |     "dob" : "2017-01-15",
           |     "details" : ${Json.toJson(lodgingOfficerDetails)}
           |   }
           |}
          """.stripMargin).as[JsObject]

      val officer = Json.obj("role" -> "director", "name" -> Json.obj(
        "forename" -> "First Name Test",
        "other_forenames" -> "Middle Name Test",
        "surname" -> "Last Name Test"
      ))

      val expectedModel = LodgingOfficer(
        dob = Some(LocalDate.of(2017,1,15)),
        nino = "JW778877A",
        role = "director",
        name = Name(
          first = Some("First Name Test"),
          middle = Some("Middle Name Test"),
          last = "Last Name Test"
        ),
        ivPassed = None,
        details = Some(lodgingOfficerDetails),
        isOfficerApplying = true
      )

      "retrieved from eligiblityData json combined with lodging officer json and officer is applying" in {
        val questions1 = Seq(
          Json.obj("questionId" -> "completionCapacity", "question" -> "Some Question 11", "answer" -> "Some Answer 11", "answerValue" -> officer),
          Json.obj("questionId" -> "foo", "question" -> "Some Question 12", "answer" -> "Some Answer 12", "answerValue" -> "val12")
        )
        val questions2 = Seq(
          Json.obj("questionId" -> "applicantUKNino-optionalData", "question" -> "Some Question 22", "answer" -> "Some Answer 22", "answerValue" -> "JW778877A"),
          Json.obj("questionId" -> "wizz", "question" -> "Some Question 22", "answer" -> "Some Answer 22", "answerValue" -> "val22")
        )
        val section1 = Json.obj("title" -> "test TITLE 1", "data" -> JsArray(questions1))
        val section2 = Json.obj("title" -> "test TITLE 2", "data" -> JsArray(questions2))
        val sections = JsArray(Seq(section1, section2))
        val eligibilityData = Json.obj("eligibilityData" -> Json.obj("sections" -> sections))

        val res = Json.fromJson[LodgingOfficer](EligibilityDataJsonUtils.toJsObject(eligibilityData) ++ lodgingOfficerJson)(LodgingOfficer.mongoReads)

        res mustBe JsSuccess(expectedModel)
      }

      "retrieved from eligiblityData json combined with lodging officer json with completionCapacity = noneofthese" in {
        val questions1 = Seq(
          Json.obj("questionId" -> "completionCapacity", "question" -> "Some Question 11", "answer" -> "Some Answer 11", "answerValue" -> "NoNeOfThese"),
          Json.obj("questionId" -> "foo", "question" -> "Some Question 12", "answer" -> "Some Answer 12", "answerValue" -> "val12")
        )
        val questions2 = Seq(
          Json.obj("questionId" -> "completionCapacityFillingInFor", "question" -> "Some Question 21", "answer" -> "Some Answer 21", "answerValue" -> officer),
          Json.obj("questionId" -> "applicantUKNino-optionalData", "question" -> "Some Question 22", "answer" -> "Some Answer 22", "answerValue" -> "JW778877A"),
          Json.obj("questionId" -> "wizz", "question" -> "Some Question 22", "answer" -> "Some Answer 22", "answerValue" -> "val22")
        )
        val section1 = Json.obj("title" -> "test TITLE 1", "data" -> JsArray(questions1))
        val section2 = Json.obj("title" -> "test TITLE 2", "data" -> JsArray(questions2))
        val sections = JsArray(Seq(section1, section2))
        val eligibilityData = Json.obj("eligibilityData" -> Json.obj("sections" -> sections))

        val res = Json.fromJson[LodgingOfficer](EligibilityDataJsonUtils.toJsObject(eligibilityData) ++ lodgingOfficerJson)(LodgingOfficer.mongoReads)

        res mustBe JsSuccess(expectedModel.copy(isOfficerApplying = false))
      }

      "return jsError when CompletionCapacity = nonofthese but there is no CompletionCapacityFillingInFor" in {
        val questions1 = Seq(
          Json.obj("questionId" -> "completionCapacity", "question" -> "Some Question 11", "answer" -> "Some Answer 11", "answerValue" -> "noneOFTHESE"),
          Json.obj("questionId" -> "foo", "question" -> "Some Question 12", "answer" -> "Some Answer 12", "answerValue" -> "val12")
        )
        val questions2 = Seq(
          Json.obj("questionId" -> "applicantUKNino", "question" -> "Some Question 22", "answer" -> "Some Answer 22", "answerValue" -> "JW778877A"),
          Json.obj("questionId" -> "wizz", "question" -> "Some Question 22", "answer" -> "Some Answer 22", "answerValue" -> "val22")
        )
        val section1 = Json.obj("title" -> "test TITLE 1", "data" -> JsArray(questions1))
        val section2 = Json.obj("title" -> "test TITLE 2", "data" -> JsArray(questions2))
        val sections = JsArray(Seq(section1, section2))
        val eligibilityData = Json.obj("eligibilityData" -> Json.obj("sections" -> sections))

        val res = Json.fromJson[LodgingOfficer](EligibilityDataJsonUtils.toJsObject(eligibilityData) ++ lodgingOfficerJson)(LodgingOfficer.mongoReads)

        res.isError mustBe true
      }

      "return jsError when CompletionCapacity has an incorrect value" in {
        val questions1 = Seq(
          Json.obj("questionId" -> "completionCapacity", "question" -> "Some Question 11", "answer" -> "Some Answer 11", "answerValue" -> 1234),
          Json.obj("questionId" -> "foo", "question" -> "Some Question 12", "answer" -> "Some Answer 12", "answerValue" -> "val12")
        )
        val questions2 = Seq(
          Json.obj("questionId" -> "applicantUKNino", "question" -> "Some Question 22", "answer" -> "Some Answer 22", "answerValue" -> "JW778877A"),
          Json.obj("questionId" -> "wizz", "question" -> "Some Question 22", "answer" -> "Some Answer 22", "answerValue" -> "val22")
        )
        val section1 = Json.obj("title" -> "test TITLE 1", "data" -> JsArray(questions1))
        val section2 = Json.obj("title" -> "test TITLE 2", "data" -> JsArray(questions2))
        val sections = JsArray(Seq(section1, section2))
        val eligibilityData = Json.obj("eligibilityData" -> Json.obj("sections" -> sections))

        val res = Json.fromJson[LodgingOfficer](EligibilityDataJsonUtils.toJsObject(eligibilityData) ++ lodgingOfficerJson)(LodgingOfficer.mongoReads)
        res.isError mustBe true
      }
    }
  }

  "eligibilityDataJsonReads" should {
    val thresholdPreviousThirtyDays = LocalDate.of(2017, 5, 23)
    val thresholdInTwelveMonths = LocalDate.of(2017, 7, 16)
    val officer = Json.obj("role" -> "director", "name" -> Json.obj(
      "forename" -> "First Name Test",
      "other_forenames" -> "Middle Name Test",
      "surname" -> "Last Name Test"
    ))
    val json = Json.obj(
      "thresholdPreviousThirtyDays" -> thresholdPreviousThirtyDays,
      "thresholdInTwelveMonths" -> thresholdInTwelveMonths,
      "turnoverEstimate" -> 2024,
      "applicantUKNino-optionalData" -> "JW778877A",
      "completionCapacity" -> officer,
      "fooDirectorDetails3" -> true
    )

    "return JsSuccess" when {
      "completionCapacity is defined with a JsObject" in {
        val res = Json.fromJson(json)(LodgingOfficer.eligibilityDataJsonReads)
        res mustBe JsSuccess(("JW778877A", Name(first = Some("First Name Test"), middle = Some("Middle Name Test"), last = "Last Name Test"), "director", true))
      }
      "completionCapacity is defined with noneofthese and completionCapacityFillingInFor is defined with a JsObject" in {
        val rep = Json.obj("role" -> "director", "name" -> Json.obj(
          "forename" -> "First Name Test 2",
          "other_forenames" -> "Middle Name Test 2",
          "surname" -> "Last Name Test 2"
        ))

        val json2 = json - "completionCapacity" ++ Json.obj("completionCapacity" -> "noneofthese", "completionCapacityFillingInFor" -> rep)
        val res = Json.fromJson(json2)(LodgingOfficer.eligibilityDataJsonReads)
        val expected = ("JW778877A", Name(first = Some("First Name Test 2"), middle = Some("Middle Name Test 2"), last = "Last Name Test 2"), "director", false)

        res mustBe JsSuccess(expected)
      }
    }

    "return JsError" when {
      "completionCapacity is not defined" in {
        val testJson = json - "completionCapacity"
        val res = Json.fromJson(testJson)(LodgingOfficer.eligibilityDataJsonReads)
        res.isError mustBe true
      }
      "completionCapacity is not defined correctly" in {
        val testJson = json - "completionCapacity" ++ Json.obj("completionCapacity" -> 125454)
        val res = Json.fromJson(testJson)(LodgingOfficer.eligibilityDataJsonReads)
        res.isError mustBe true
      }
      "completionCapacity is defined with noneofthese but completionCapacityFillingInFor is missing" in {
        val testJson = json - "completionCapacity" ++ Json.obj("completionCapacity" -> "noneofthese")
        val res = Json.fromJson(testJson)(LodgingOfficer.eligibilityDataJsonReads)
        res.isError mustBe true
      }
      "completionCapacity is defined with noneofthese but completionCapacityFillingInFor is not defined correctly" in {
        val testJson = json - "completionCapacity" ++ Json.obj("completionCapacity" -> "noneofthese", "completionCapacityFillingInFor" -> 4564654)
        val res = Json.fromJson(testJson)(LodgingOfficer.eligibilityDataJsonReads)
        res.isError mustBe true
      }
      "applicantUKNino is not valid" in {
        val testJson = json - "applicantUKNino" ++ Json.obj("applicantUKNino-optionalData" -> "SF123456E")
        val res = Json.fromJson(testJson)(LodgingOfficer.eligibilityDataJsonReads)
        res.isError mustBe true
      }
      "officer role is not valid" in {
        val invalidOfficer = Json.obj("role" -> "manager", "name" -> Json.obj(
          "forename" -> "First Name Test",
          "other_forenames" -> "Middle Name Test",
          "surname" -> "Last Name Test"
        ))

        val testJson = json - "completionCapacity" ++ Json.obj("completionCapacity" -> invalidOfficer)
        val res = Json.fromJson(testJson)(LodgingOfficer.eligibilityDataJsonReads)

        res.isError mustBe true
      }
    }
  }

  "patchJsonReads" should {
    "return JsSuccess" when {
      "full json is defined" in {
        val json = Json.obj(
          "dob" -> "2015-05-27",
          "ivPassed" -> true,
          "details" -> Json.toJson(lodgingOfficerDetails).as[JsObject]
        )

        val res = Json.fromJson(json)(LodgingOfficer.patchJsonReads)
        res mustBe JsSuccess(json)
      }
      "min json is defined" in {
        val json = Json.obj(
          "dob" -> "2015-05-27"
        )

        val res = Json.fromJson(json)(LodgingOfficer.patchJsonReads)
        res mustBe JsSuccess(json)
      }
    }
    "return JsError" when {
      "dob is missing" in {
        val json = Json.obj(
          "ivPassed" -> true,
          "details" -> Json.toJson(lodgingOfficerDetails).as[JsObject]
        )

        val res = Json.fromJson(json)(LodgingOfficer.patchJsonReads)
        res.isError mustBe true
      }
      "ivPassed is not correct" in {
        val json = Json.obj(
          "dob" -> "2015-05-27",
          "ivPassed" -> "wrong value",
          "details" -> Json.toJson(lodgingOfficerDetails).as[JsObject]
        )

        val res = Json.fromJson(json)(LodgingOfficer.patchJsonReads)
        res.isError mustBe true
      }
      "details is not correct" in {
        val json = Json.obj(
          "dob" -> "2015-05-27",
          "ivPassed" -> true,
          "details" -> (Json.toJson(lodgingOfficerDetails).as[JsObject] - "contact")
        )

        val res = Json.fromJson(json)(LodgingOfficer.patchJsonReads)
        res.isError mustBe true
      }
    }
  }
}

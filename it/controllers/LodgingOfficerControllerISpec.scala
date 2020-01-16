
package controllers

import java.time.LocalDate

import com.github.tomakehurst.wiremock.stubbing.StubMapping
import itutil.IntegrationStubbing
import models.api._
import play.api.libs.json.{JsArray, JsBoolean, JsObject, Json}
import play.api.test.Helpers._
import controllers.routes.LodgingOfficerController

import scala.concurrent.ExecutionContext.Implicits.global

class LodgingOfficerControllerISpec extends IntegrationStubbing {

  class Setup extends SetupHelper {
    def writeAudit: StubMapping = stubPost("/write/audit/merged",200,"")
  }

  val currentAddress            = Address("12 Lukewarm","Oriental lane")
  val skylakeValiarm            = Name(first = Some("Skylake"), middle = None, last = "Valiarm")
  val skylakeDigitalContact     = DigitalContactOptional(None, Some("123456789012345678"), None)
  val lodgingOfficerDetails     = LodgingOfficerDetails(currentAddress = currentAddress, None, None, contact = skylakeDigitalContact)
  val validLodgingOfficerPreIV  = LodgingOfficer(
    dob = Some(LocalDate.of(1980, 5, 25)),
    nino = "AB123456A",
    role = "secretary",
    name = skylakeValiarm,
    ivPassed = None,
    details = None)
  val validLodgingOfficerPostIv = validLodgingOfficerPreIV.copy(details = Some(lodgingOfficerDetails))

  def vatScheme(regId: String): VatScheme = emptyVatScheme(regId).copy(lodgingOfficer = Some(validLodgingOfficerPreIV))

  val upsertLodgingOfficerJson = Json.parse(
    s"""
       |{
       | "name": {
       |   "first" : "Skylake",
       |   "last" : "Valiarm"
       | },
       | "dob" : "1980-05-25",
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
       | "dob" : "1980-05-25",
       | "nino" : "AB123456A",
       | "role" : "secretary",
       | "isOfficerApplying": true
       |}
    """.stripMargin).as[JsObject]

  val invalidLodgingOfficerJson = Json.parse(
    s"""
       |{
       | "nino" : "AB123456A",
       | "role" : "secretary"
       |}
    """.stripMargin).as[JsObject]

  val completionCapacity = Json.obj("role" -> "secretary", "name" -> Json.obj(
    "forename" -> "Skylake",
    "surname" -> "Valiarm"
  ))
  val questions1 = Seq(
    Json.obj("questionId" -> "completionCapacity", "question" -> "Some Question 11", "answer" -> "Some Answer 11", "answerValue" -> completionCapacity),
    Json.obj("questionId" -> "testQId12", "question" -> "Some Question 12", "answer" -> "Some Answer 12", "answerValue" -> "val12")
  )
  val questions2 = Seq(
    Json.obj("questionId" -> "applicantUKNino-optionalData", "question" -> "Some Question 22", "answer" -> "Some Answer 22", "answerValue" -> "AB123456A"),
    Json.obj("questionId" -> "testQId21", "question" -> "Some Question 21", "answer" -> "Some Answer 21", "answerValue" -> "val21"),
    Json.obj("questionId" -> "testQId22", "question" -> "Some Question 22", "answer" -> "Some Answer 22", "answerValue" -> "val22")
  )
  val section1 = Json.obj("title" -> "test TITLE 1", "data" -> JsArray(questions1))
  val section2 = Json.obj("title" -> "test TITLE 2", "data" -> JsArray(questions2))
  val sections = JsArray(Seq(section1, section2))
  val eligibilityData = Json.obj("sections" -> sections)

  "getLodgingOfficerData" should {
    "return OK" in new Setup {
      given.user.isAuthorised

      insertIntoDb(vatScheme("regId").copy(eligibilityData = Some(eligibilityData)))

      val response = await(client(LodgingOfficerController.getLodgingOfficerData("regId").url).get())

      response.status shouldBe OK
      response.json shouldBe validLodgingOfficerJson
    }

    "return NO_CONTENT" in new Setup {
      given.user.isAuthorised

      insertIntoDb(emptyVatScheme("regId"))

      val response = await(client(LodgingOfficerController.getLodgingOfficerData("regId").url).get())

      response.status shouldBe NO_CONTENT
    }

    "return NOT_FOUND if no document found" in new Setup {
      given.user.isAuthorised

      val response = await(client(LodgingOfficerController.getLodgingOfficerData("regId").url).get())

      response.status shouldBe NOT_FOUND
    }

    "return FORBIDDEN if user is not authorised" in new Setup {
      given.user.isNotAuthorised

      val response = await(client(LodgingOfficerController.getLodgingOfficerData("regId").url).get())

      response.status shouldBe FORBIDDEN
    }
  }

  "updateLodgingOfficerData" should {
    "return OK with a lodgingOfficer json body" in new Setup {
      given.user.isAuthorised

      insertIntoDb(emptyVatScheme("regId").copy(eligibilityData = Some(eligibilityData)))

      val response = await(client(LodgingOfficerController.updateLodgingOfficerData("regId").url)
        .patch(validLodgingOfficerJson))

      response.status shouldBe OK
      response.json shouldBe Json.parse("""{"dob" : "1980-05-25"}""".stripMargin)
    }

    "return OK with when updating the lodging officer post IV" in new Setup {
      given.user.isAuthorised

      insertIntoDb(vatScheme("regId").copy(eligibilityData = Some(eligibilityData)))

      val response = await(client(LodgingOfficerController.updateLodgingOfficerData("regId").url)
        .patch(upsertLodgingOfficerJson))

      response.status shouldBe OK
      response.json shouldBe upsertLodgingOfficerJson - "name" - "nino" - "role" - "isOfficerApplying"
    }

    "return BAD_REQUEST if an invalid json body is posted" in new Setup {
      given.user.isAuthorised

      insertIntoDb(emptyVatScheme("regId"))

      val response = await(client(LodgingOfficerController.updateLodgingOfficerData("regId").url)
        .patch(invalidLodgingOfficerJson))

      response.status shouldBe BAD_REQUEST
    }

    "return NOT_FOUND if no reg document is found" in new Setup {
      given.user.isAuthorised

      val response = await(client(LodgingOfficerController.updateLodgingOfficerData("regId").url)
        .patch(validLodgingOfficerJson))

      response.status shouldBe NOT_FOUND
    }

    "return OK if no data updated because data is same" in new Setup {
      given.user.isAuthorised

      insertIntoDb(vatScheme("regId").copy(eligibilityData = Some(eligibilityData)))

      val response = await(client(LodgingOfficerController.updateLodgingOfficerData("regId").url)
        .patch(validLodgingOfficerJson))

      response.status shouldBe OK
    }

    "return FORBIDDEN if user is not authorised obtained" in new Setup {
      given.user.isNotAuthorised

      val response = await(client(LodgingOfficerController.updateLodgingOfficerData("regId").url)
        .patch(validLodgingOfficerJson))

      response.status shouldBe FORBIDDEN
    }
  }

  "updateIVStatus" should {
    "return OK" in new Setup {
      given.user.isAuthorised

      insertIntoDb(vatScheme("regId"))

      val response = await(client(LodgingOfficerController.updateIVStatus("regId", true).url)
        .patch(""))

      response.status shouldBe OK
      response.json shouldBe JsBoolean(true)
    }

    "return NOT_FOUND if a none boolean is provided in the query parameter" in new Setup {
      given.user.isAuthorised

      insertIntoDb(emptyVatScheme("regId"))

      val response = await(client("/regId/update-iv-status/test").patch(""))

      response.status shouldBe NOT_FOUND
    }

    "return NOT_FOUND if no reg document is found" in new Setup {
      given.user.isAuthorised

      val response = await(client(LodgingOfficerController.updateIVStatus("regId", true).url).patch(""))

      response.status shouldBe NOT_FOUND
    }

    "return NOT_FOUND if the reg document has no lodgingOfficer block" in new Setup {
      given.user.isAuthorised

      insertIntoDb(emptyVatScheme("regId"))

      val response = await(client(LodgingOfficerController.updateIVStatus("regId", true).url)
        .patch(""))

      response.status shouldBe NOT_FOUND
    }

    "return FORBIDDEN if user is not authorised obtained" in new Setup {
      given.user.isNotAuthorised

      val response = await(client(LodgingOfficerController.updateIVStatus("regId", true).url)
        .patch(""))

      response.status shouldBe FORBIDDEN
    }
  }
}

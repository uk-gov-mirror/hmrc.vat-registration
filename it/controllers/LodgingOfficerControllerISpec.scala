
package controllers

import java.time.LocalDate

import com.github.tomakehurst.wiremock.stubbing.StubMapping
import itutil.IntegrationStubbing
import models.api._
import play.api.libs.json.{JsArray, JsBoolean, JsObject, Json}
import play.api.test.Helpers._
import controllers.routes.LodgingOfficerController
import play.api.libs.ws.WSResponse

import scala.concurrent.ExecutionContext.Implicits.global

class LodgingOfficerControllerISpec extends IntegrationStubbing {

  class Setup extends SetupHelper {
    def writeAudit: StubMapping = stubPost("/write/audit/merged",200,"")
  }

  val currentAddress: Address                       = Address("12 Lukewarm","Oriental lane")
  val skylakeValiarm: Name                          = Name(first = Some("Skylake"), middle = None, last = "Valiarm")
  val skylakeDigitalContact: DigitalContactOptional = DigitalContactOptional(None, Some("123456789012345678"), None)
  val lodgingOfficerDetails: LodgingOfficerDetails  = LodgingOfficerDetails(currentAddress = currentAddress, None, None, contact = skylakeDigitalContact)
  val validLodgingOfficer: LodgingOfficer           = LodgingOfficer(
    nino = "AB123456A",
    role = "secretary",
    name = skylakeValiarm,
    details = None)
  val validLodgingOfficerPostIv: LodgingOfficer = validLodgingOfficer.copy(details = Some(lodgingOfficerDetails))

  def vatScheme(regId: String): VatScheme = emptyVatScheme(regId)

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

  val completionCapacity: JsObject = Json.obj("role" -> "secretary", "name" -> Json.obj(
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
  val section1: JsObject = Json.obj("title" -> "test TITLE 1", "data" -> JsArray(questions1))
  val section2: JsObject = Json.obj("title" -> "test TITLE 2", "data" -> JsArray(questions2))
  val sections: JsArray = JsArray(Seq(section1, section2))
  val eligibilityData: JsObject = Json.obj("sections" -> sections)

  //TODO - remove when applicant data is recorded as part of other services
  "getLodgingOfficerData" ignore {
    "return OK" in new Setup {
      given.user.isAuthorised

      insertIntoDb(vatScheme("regId").copy(eligibilityData = Some(eligibilityData)))

      val response: WSResponse = await(client(LodgingOfficerController.getLodgingOfficerData("regId").url).get())

      response.status mustBe OK
      response.json mustBe validLodgingOfficerJson
    }

    "return NO_CONTENT" in new Setup {
      given.user.isAuthorised

      insertIntoDb(emptyVatScheme("regId"))

      val response: WSResponse = await(client(LodgingOfficerController.getLodgingOfficerData("regId").url).get())

      response.status mustBe NO_CONTENT
    }

    "return NOT_FOUND if no document found" in new Setup {
      given.user.isAuthorised

      val response: WSResponse = await(client(LodgingOfficerController.getLodgingOfficerData("regId").url).get())

      response.status mustBe NOT_FOUND
    }

    "return FORBIDDEN if user is not authorised" in new Setup {
      given.user.isNotAuthorised

      val response: WSResponse = await(client(LodgingOfficerController.getLodgingOfficerData("regId").url).get())

      response.status mustBe FORBIDDEN
    }
  }

  //TODO - remove when applicant data is recorded as part of other services
  "updateLodgingOfficerData" ignore {
    "return OK with a lodgingOfficer json body" in new Setup {
      given.user.isAuthorised

      insertIntoDb(emptyVatScheme("regId").copy(eligibilityData = Some(eligibilityData)))

      val response: WSResponse = await(client(LodgingOfficerController.updateLodgingOfficerData("regId").url)
        .patch(validLodgingOfficerJson))

      response.status mustBe OK
      response.json mustBe Json.parse("""{}""".stripMargin)
    }

    "return BAD_REQUEST if an invalid json body is posted" in new Setup {
      given.user.isAuthorised

      insertIntoDb(emptyVatScheme("regId"))

      val response: WSResponse = await(client(LodgingOfficerController.updateLodgingOfficerData("regId").url)
        .patch(invalidLodgingOfficerJson))

      response.status mustBe BAD_REQUEST
    }

    "return NOT_FOUND if no reg document is found" in new Setup {
      given.user.isAuthorised

      val response: WSResponse = await(client(LodgingOfficerController.updateLodgingOfficerData("regId").url)
        .patch(validLodgingOfficerJson))

      response.status mustBe NOT_FOUND
    }

    "return OK if no data updated because data is same" in new Setup {
      given.user.isAuthorised

      insertIntoDb(vatScheme("regId").copy(eligibilityData = Some(eligibilityData)))

      val response: WSResponse = await(client(LodgingOfficerController.updateLodgingOfficerData("regId").url)
        .patch(validLodgingOfficerJson))

      response.status mustBe OK
    }

    "return FORBIDDEN if user is not authorised obtained" in new Setup {
      given.user.isNotAuthorised

      val response: WSResponse = await(client(LodgingOfficerController.updateLodgingOfficerData("regId").url)
        .patch(validLodgingOfficerJson))

      response.status mustBe FORBIDDEN
    }
  }

}

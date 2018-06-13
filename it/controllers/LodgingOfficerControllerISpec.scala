
package controllers

import java.time.LocalDate

import com.github.tomakehurst.wiremock.stubbing.StubMapping
import itutil.{IntegrationStubbing, WiremockHelper}
import models.api._
import play.api.libs.json.{JsArray, JsBoolean, JsObject, Json}
import play.api.test.FakeApplication

import scala.concurrent.ExecutionContext.Implicits.global

class LodgingOfficerControllerISpec extends IntegrationStubbing {

  val mockHost = WiremockHelper.wiremockHost
  val mockPort = WiremockHelper.wiremockPort
  val mockUrl  = s"http://$mockHost:$mockPort"

  override implicit lazy val app = FakeApplication(additionalConfiguration = Map(
    "auditing.consumer.baseUri.host" -> s"$mockHost",
    "auditing.consumer.baseUri.port" -> s"$mockPort",
    "microservice.services.auth.host" -> s"$mockHost",
    "microservice.services.auth.port" -> s"$mockPort",
    "microservice.services.des-stub.host" -> s"$mockHost",
    "microservice.services.des-stub.port" -> s"$mockPort",
    "microservice.services.company-registration.host" -> s"$mockHost",
    "microservice.services.company-registration.port" -> s"$mockPort",
    "microservice.services.incorporation-information.host" -> s"$mockHost",
    "microservice.services.incorporation-information.port" -> s"$mockPort",
    "microservice.services.incorporation-information.uri" -> "/incorporation-information",
    "mongo-encryption.key" -> "ABCDEFGHIJKLMNOPQRSTUV=="
  ))

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
    "return 200" in new Setup {
      given
        .user.isAuthorised

      insertIntoDb(vatScheme("regId").copy(eligibilityData = Some(eligibilityData)))

      await(client(controllers.routes.LodgingOfficerController.getLodgingOfficerData("regId").url).get() map { response =>
        response.status shouldBe 200
        response.json shouldBe validLodgingOfficerJson
      })
    }

    "return 204" in new Setup {
      given
        .user.isAuthorised

      insertIntoDb(emptyVatScheme("regId"))

      await(client(controllers.routes.LodgingOfficerController.getLodgingOfficerData("regId").url).get() map { response =>
        response.status shouldBe 204
      })
    }

    "return 404 if no document found" in new Setup {
      given
        .user.isAuthorised

      await(client(controllers.routes.LodgingOfficerController.getLodgingOfficerData("regId").url).get() map { response =>
        response.status shouldBe 404
      })
    }

    "return 403 if user is not authorised" in new Setup {
      given
        .user.isNotAuthorised

      await(client(controllers.routes.LodgingOfficerController.getLodgingOfficerData("regId").url).get() map { response =>
        response.status shouldBe 403
      })
    }
  }

  "updateLodgingOfficerData" should {
    "return 200 with a lodgingOfficer json body" in new Setup {
      given
        .user.isAuthorised

      insertIntoDb(emptyVatScheme("regId").copy(eligibilityData = Some(eligibilityData)))

      await(client(controllers.routes.LodgingOfficerController.updateLodgingOfficerData("regId").url).patch(validLodgingOfficerJson) map { response =>
        response.status shouldBe 200
        response.json shouldBe Json.parse("""{"dob" : "1980-05-25"}""".stripMargin)
      })
    }

    "return 200 with when updating the lodging officer post IV" in new Setup {
      given
        .user.isAuthorised

      insertIntoDb(vatScheme("regId").copy(eligibilityData = Some(eligibilityData)))

      await(client(controllers.routes.LodgingOfficerController.updateLodgingOfficerData("regId").url).patch(upsertLodgingOfficerJson) map { response =>
        response.status shouldBe 200
        response.json shouldBe upsertLodgingOfficerJson - "name" - "nino" - "role" - "isOfficerApplying"
      })
    }

    "return 400 if an invalid json body is posted" in new Setup {
      given
        .user.isAuthorised

      insertIntoDb(emptyVatScheme("regId"))

      await(client(controllers.routes.LodgingOfficerController.updateLodgingOfficerData("regId").url).patch(invalidLodgingOfficerJson) map { response =>
        response.status shouldBe 400
      })
    }

    "return 404 if no reg document is found" in new Setup {
      given
        .user.isAuthorised

      await(client(controllers.routes.LodgingOfficerController.updateLodgingOfficerData("regId").url).patch(validLodgingOfficerJson) map { response =>
        response.status shouldBe 404
      })
    }

    "return 200 if no data updated because data is same" in new Setup {
      given
        .user.isAuthorised

      insertIntoDb(vatScheme("regId").copy(eligibilityData = Some(eligibilityData)))

      await(client(controllers.routes.LodgingOfficerController.updateLodgingOfficerData("regId").url).patch(validLodgingOfficerJson) map { response =>
        response.status shouldBe 200
      })
    }

    "return 403 if user is not authorised obtained" in new Setup {
      given
        .user.isNotAuthorised

      await(client(controllers.routes.LodgingOfficerController.updateLodgingOfficerData("regId").url).patch(validLodgingOfficerJson) map { response =>
        response.status shouldBe 403
      })
    }
  }

  "updateIVStatus" should {
    "return 200" in new Setup {
      given
        .user.isAuthorised

      insertIntoDb(vatScheme("regId"))

      await(client(controllers.routes.LodgingOfficerController.updateIVStatus("regId", true).url).patch("") map { response =>
        response.status shouldBe 200
        response.json shouldBe JsBoolean(true)
      })
    }

    "return 404 if a none boolean is provided in the query parameter" in new Setup {
      given
        .user.isAuthorised

      insertIntoDb(emptyVatScheme("regId"))

      await(client("/regId/update-iv-status/test").patch("") map { response =>
        response.status shouldBe 404
      })
    }

    "return 404 if no reg document is found" in new Setup {
      given
        .user.isAuthorised

      await(client(controllers.routes.LodgingOfficerController.updateIVStatus("regId", true).url).patch("") map { response =>
        response.status shouldBe 404
      })
    }

    "return 404 if the reg document has no lodgingOfficer block" in new Setup {
      given
        .user.isAuthorised

      insertIntoDb(emptyVatScheme("regId"))

      await(client(controllers.routes.LodgingOfficerController.updateIVStatus("regId", true).url).patch("") map { response =>
        response.status shouldBe 404
      })
    }

    "return 403 if user is not authorised obtained" in new Setup {
      given
        .user.isNotAuthorised

      await(client(controllers.routes.LodgingOfficerController.updateIVStatus("regId", true).url).patch("") map { response =>
        response.status shouldBe 403
      })
    }
  }
}
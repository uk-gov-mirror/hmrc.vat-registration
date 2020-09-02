
package controllers.test

import connectors.stubs.AuditStub.stubAudit
import controllers.test.routes.StubDesSubmissionController
import itutil.IntegrationStubbing
import models.api.Address
import play.api.libs.json.{JsValue, Json}
import play.api.libs.ws.WSResponse
import play.api.test.Helpers._

class StubDesSubmissionControllerISpec extends IntegrationStubbing {

  val testMessageType = "SubmissionCreate"
  val testCustomerStatus = "3"
  val testTradersPartyType = "50"
  val testSafeID = "12345678901234567890"
  val testLine1 = "line1"
  val testLine2 = "line2"
  val testPostCode = "A11 11A"
  val testCountry = "GB"
  val testAddress: Address = Address(
    line1 = testLine1,
    line2 = testLine2,
    postcode = Some(testPostCode),
    country = Some(testCountry)
  )

  val submissionJson: JsValue = Json.obj(
    "messageType" -> testMessageType,
    "admin" -> Json.obj(
      "additionalInformation" -> Json.obj(
        "customerStatus" -> testCustomerStatus
      )
    ),
    "customerIdentification" -> Json.obj(
      "tradersPartyType" -> testTradersPartyType,
      "primeBPSafeId" -> testSafeID
    ),
    "contact" -> Json.obj(
      "address" -> Json.obj(
        "line1" -> testLine1,
        "line2" -> testLine2,
        "postCode" -> testPostCode,
        "countryCode" -> testCountry
      )
    ),
    "declaration" -> Json.obj(
      "declarationSigning" -> Json.obj(
        "confirmInformationDeclaration" -> true
      )
    )
  )

  class Setup extends SetupHelper()

  "processSubmission" should {
    "return OK if the json is a valid VatSubmission" in new Setup() {
      stubAudit(OK)

      val response: WSResponse = await(client(StubDesSubmissionController.processSubmission().url).post(submissionJson))

      response.status mustBe OK
    }

    "fail if the json is not a valid VatSubmission" in new Setup() {
      stubAudit(OK)

      val response: WSResponse = await(client(StubDesSubmissionController.processSubmission().url).post(""))

      response.status mustBe UNSUPPORTED_MEDIA_TYPE
    }
  }
}

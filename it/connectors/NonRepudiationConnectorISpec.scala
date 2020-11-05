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

package connectors

import connectors.stubs.NonRepudiationStub._
import itutil.IntegrationStubbing
import models.nonrepudiation.{NonRepudiationMetadata, NonRepudiationSubmissionAccepted}
import play.api.libs.json.{JsObject, Json}
import play.api.test.Helpers._

class NonRepudiationConnectorISpec extends IntegrationStubbing {
  val testNonRepudiationApiKey = "testNonRepudiationApiKey"
  override lazy val additionalConfig = Map("microservice.services.non-repudiation.api-key" -> testNonRepudiationApiKey)

  import AuthTestData._

  lazy val connector: NonRepudiationConnector = app.injector.instanceOf[NonRepudiationConnector]

  "submitNonRepudiation" when {
    "the non-repudiation service returns a success" should {
      s"return $NonRepudiationSubmissionAccepted" in {
        val testEncodedPayload = "testEncodedPayload"
        val testPayloadChecksum = "testPayloadChecksum"
        val testAuthToken = "testAuthToken"
        val headerData = Map("testHeaderKey" -> "testHeaderValue")
        val testPostcode = "testPostcode"

        val testNonRepudiationMetadata = NonRepudiationMetadata(
          businessId = "vrs",
          notableEvent = "vat-registration",
          payloadContentType = "application/json",
          payloadSha256Checksum = testPayloadChecksum,
          userSubmissionTimestamp = testDateTime,
          identityData = testNonRepudiationIdentityData,
          userAuthToken = testAuthToken,
          headerData = headerData,
          searchKeys = Map("postCode" -> testPostcode)
        )

        val expectedRequestJson: JsObject = Json.obj(
          "payload" -> testEncodedPayload,
          "metadata" -> testNonRepudiationMetadata
        )

        val testNonRepudiationSubmissionId = "testNonRepudiationSubmissionId"
        stubNonRepudiationSubmission(expectedRequestJson, testNonRepudiationApiKey)(ACCEPTED, Json.obj("nrSubmissionId" -> testNonRepudiationSubmissionId))

        val res = connector.submitNonRepudiation(testEncodedPayload, testNonRepudiationMetadata)

        await(res) mustBe NonRepudiationSubmissionAccepted(testNonRepudiationSubmissionId)


      }
    }
  }
}

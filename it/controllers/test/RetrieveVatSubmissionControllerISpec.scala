
package controllers.test

import itutil.{IntegrationSpecBase, IntegrationStubbing}
import org.scalatest.Matchers
import play.api.libs.json.Json

import scala.concurrent.ExecutionContext.Implicits.global
import play.api.test.Helpers._
import services.submission.SubmissionPayloadBuilder

class RetrieveVatSubmissionControllerISpec extends IntegrationSpecBase with IntegrationStubbing {

  val url = s"/vatreg/test-only/submissions/$testRegId/submission-payload"
  val builder = app.injector.instanceOf[SubmissionPayloadBuilder]

  "/test-only/submissions/:regId/submission-payload" must {
    "return OK with the submission Json" in new SetupHelper {
      given.user.isAuthorised
        .regRepo.insertIntoDb(testFullVatSchemeWithUnregisteredBusinessPartner, repo.insert)

      val expectedJson = await(builder.buildSubmissionPayload(testRegId))
      val res = client(url).get

      whenReady(res) { result =>
        result.status mustBe OK
        result.json mustBe expectedJson
      }

    }
  }

}

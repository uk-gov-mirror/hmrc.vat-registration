
package services

import itutil.{IntegrationStubbing, SubmissionAuditFixture}
import models.monitoring.SubmissionAuditModel
import services.monitoring.SubmissionAuditBlockBuilder
import uk.gov.hmrc.auth.core.AffinityGroup.Organisation

class SubmissionAuditBlockBuilderISpec extends IntegrationStubbing with SubmissionAuditFixture {

  val service = app.injector.instanceOf[SubmissionAuditBlockBuilder]

  "buildAuditJson" must {
    "return the correct JSON" in new SetupHelper {
      given
        .user.isAuthorised

      val res = service.buildAuditJson(
        vatScheme = testFullVatScheme,
        authProviderId = testAuthProviderId,
        affinityGroup = Organisation,
        optAgentReferenceNumber = None
      )

      res mustBe SubmissionAuditModel(
        userAnswers = detailBlockAnswers,
        vatScheme = testFullVatScheme,
        authProviderId = testAuthProviderId,
        affinityGroup = Organisation,
        optAgentReferenceNumber = None
      )
    }
  }

}

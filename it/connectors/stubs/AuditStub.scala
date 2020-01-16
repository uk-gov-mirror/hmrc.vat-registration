
package connectors.stubs

import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.StubMapping

object AuditStub {

  def stubAudit(status: Int): StubMapping = stubFor(post(urlEqualTo("/write/audit"))
    .willReturn(aResponse()
      .withStatus(status)
      .withBody("")
    )
  )

  def stubMergedAudit(status: Int): StubMapping = stubFor(post(urlEqualTo("/write/audit/merged"))
    .willReturn(aResponse()
      .withStatus(status)
      .withBody("")
    )
  )

}


package connectors.stubs

import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, post, stubFor, urlMatching}
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.libs.json.{JsObject, Json}

object VatSubmissionStub {

  def stubVatSubmission(status: Int)(body: JsObject = Json.obj()): StubMapping =
    stubFor(post(urlMatching(s".*/vat/subscription"))
      .willReturn(aResponse()
        .withStatus(status)
        .withBody(body.toString())
      )
    )

}


package connectors.stubs

import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.libs.json.{JsObject, Json}

object BusinessRegConnectorStub {

  def stubBusinessRegVat(status: Int)(body: JsObject = Json.obj()): StubMapping =
    stubFor(post(urlMatching(s"/business-registration/value-added-tax"))
      .willReturn(aResponse()
        .withStatus(status)
        .withBody(body.toString())
      )
    )

}

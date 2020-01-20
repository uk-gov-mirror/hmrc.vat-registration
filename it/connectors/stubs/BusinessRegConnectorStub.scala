
package connectors.stubs

import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import models.external.CurrentProfile
import play.api.libs.json.{JsObject, Json}

object BusinessRegConnectorStub {

  def stubBusinessReg(status: Int)(profile: Option[CurrentProfile] = None): StubMapping =
    stubFor(get(urlEqualTo("/business-registration/business-tax-registration"))
      .willReturn(aResponse()
        .withBody(Json.toJson(profile).toString())
        .withStatus(status)
      )
    )

  def stubBusinessRegVat(status: Int)(body: JsObject = Json.obj()): StubMapping =
    stubFor(post(urlMatching(s"/business-registration/value-added-tax"))
      .willReturn(aResponse()
        .withStatus(status)
        .withBody(body.toString())
      )
    )

}

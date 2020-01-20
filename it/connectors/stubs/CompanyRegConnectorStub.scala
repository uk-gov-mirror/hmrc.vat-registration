
package connectors.stubs

import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, get, stubFor, urlMatching}
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.libs.json.Json

object CompanyRegConnectorStub {

  def stubGetTransID(registrationId: String, transId: String,status: Int): StubMapping =
    stubFor(get(urlMatching(s"/company-registration/corporation-tax-registration/$registrationId/corporation-tax-registration"))
      .willReturn(aResponse()
        .withStatus(200)
        .withBody(Json.obj(
          "confirmationReferences" -> Json.obj(
            "transaction-id" -> transId
            )
          ).toString()
        )
      )
    )

}


package connectors.stubs

import java.time.LocalDate

import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.libs.json.Json

object IncorpInfoConnectorStub {

  val registrationID = "testRegId"
  val regime = "vat"
  val subscriber = "scrs"
  val transID = "transID"
  val crn = "crn"
  val accepted = "accepted"

  def incorpUpdate(status: String): String = s"""
    |{
    |  "SCRSIncorpStatus": {
    |    "IncorpSubscriptionKey" : {
    |      "subscriber" : "SCRS",
    |      "discriminator" : "PAYE",
    |      "transactionId" : "$transID"
    |    },
    |    "SCRSIncorpSubscription" : {
    |      "callbackUrl" : "scrs-incorporation-update-listener.service/incorp-updates/incorp-status-update"
    |    },
    |    "IncorpStatusEvent": {
    |      "status": "$status",
    |      "crn":"$crn",
    |      "incorporationDate":1470351600000,
    |      "timestamp" : ${Json.toJson(LocalDate.of(2017, 12, 21))}
    |    }
    |  }
    |}
    """.stripMargin

  val returnedFromII = s"""
    |{
    |  "company_name":"test"
    |}
    """.stripMargin

  def stubIncorpUpdate(): StubMapping =
    stubFor(post(urlMatching(s"/incorporation-information/subscribe/$transID/regime/$regime/subscriber/$subscriber"))
      .willReturn(aResponse()
        .withStatus(200)
        .withBody(incorpUpdate(accepted))
      )
    )

  def stubNoIncorpUpdate(): StubMapping =
    stubFor(post(urlMatching(s"/incorporation-information/subscribe/$transID/regime/$regime/subscriber/$subscriber"))
      .willReturn(aResponse().withStatus(202))
    )

  def stubGetCompanyProfile(): StubMapping =
    stubFor(get(urlMatching(s"/incorporation-information/$transID/company-profile"))
      .willReturn(aResponse()
        .withStatus(200)
        .withBody(returnedFromII)
      )
    )

}

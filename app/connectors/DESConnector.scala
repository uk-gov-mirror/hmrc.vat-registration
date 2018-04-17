/*
 * Copyright 2018 HM Revenue & Customs
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

import javax.inject.{Inject, Singleton}

import config.{MicroserviceAuditConnector, WSHttp}
import models.submission.{DESSubmission, TopUpSubmission}
import play.api.Logger
import play.api.libs.json.Writes
import uk.gov.hmrc.play.config.ServicesConfig

import scala.concurrent.{ExecutionContext, Future}
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.logging.Authorization

@Singleton
class DESConnectorImpl @Inject()() extends DESConnector with ServicesConfig {
  lazy val desStubUrl: String = baseUrl("des-stub")
  lazy val desStubURI: String = getConfString("des-stub.uri", "")
  lazy val desStubTopUpUrl: String = baseUrl("des-stub")
  lazy val desStubTopUpURI: String = getConfString("des-stub.uri", "")

  lazy val urlHeaderEnvironment: String = getConfString("des-service.environment", throw new Exception("could not find config value for des-service.environment"))
  lazy val urlHeaderAuthorization: String = s"Bearer ${getConfString("des-service.authorization-token",
    throw new Exception("could not find config value for des-service.authorization-token"))}"

  val http : CorePost = WSHttp
  val auditConnector = MicroserviceAuditConnector
}

trait DESConnector extends HttpErrorFunctions {

  val desStubUrl: String
  val desStubURI: String
  val desStubTopUpUrl: String
  val desStubTopUpURI: String

  val urlHeaderEnvironment: String
  val urlHeaderAuthorization: String

  val http: CorePost

  private[connectors] def customDESRead(http: String, url: String, response: HttpResponse): HttpResponse = {
    response.status match {
      case 409 =>
        Logger.warn("[DesConnector] [customDESRead] Received 409 from DES - converting to 202")
        HttpResponse(202, Option(response.json), response.allHeaders, Option(response.body))
      case 499 =>
        Logger.warn("[DesConnector] [customDESRead] Received 499 from DES - converting to 502")
        throw Upstream5xxResponse("Timeout received from DES submission", 499, 502)
      case status if is4xx(status) =>
        throw Upstream4xxResponse(upstreamResponseMessage(http, url, status, response.body), status, reportAs = 400, response.allHeaders)
      case _ => handleResponse(http, url)(response)
    }
  }

  implicit val httpRds = new HttpReads[HttpResponse] {
    def read(http: String, url: String, res: HttpResponse) = customDESRead(http, url, res)
  }

  def submitToDES(submission: DESSubmission, regId: String)(implicit hc: HeaderCarrier): Future[HttpResponse] = {
    val url = s"$desStubUrl/$desStubURI"
    vatPOST[DESSubmission](url, submission) map { resp =>
      resp
    }
  }

  def submitTopUpToDES(submission: TopUpSubmission, regId: String)(implicit hc: HeaderCarrier): Future[HttpResponse] = {
    val url = s"$desStubTopUpUrl/$desStubTopUpURI"
    vatPOST[TopUpSubmission](url, submission) map { resp =>
      resp
    }
  }

  private def vatPOST[I](url: String, body: I, headers: Seq[(String, String)] = Seq.empty)
                           (implicit wts: Writes[I], rds: HttpReads[HttpResponse], hc: HeaderCarrier, ec: ExecutionContext) =
    http.POST[I, HttpResponse](url, body, headers)(wts = wts, rds = rds, hc = createHeaderCarrier(hc), ec = ec)

  private def createHeaderCarrier(headerCarrier: HeaderCarrier): HeaderCarrier = {
    headerCarrier.
      withExtraHeaders("Environment" -> urlHeaderEnvironment).
      copy(authorization = Some(Authorization(urlHeaderAuthorization)))
  }

}

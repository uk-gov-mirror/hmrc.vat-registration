/*
 * Copyright 2017 HM Revenue & Customs
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

import javax.inject.Singleton

import config.{MicroserviceAuditConnector, WSHttp}
import models.submission.DESSubmission
import play.api.libs.json.Writes
import uk.gov.hmrc.play.config.ServicesConfig

import scala.concurrent.{ExecutionContext, Future}
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.logging.Authorization

@Singleton
class DESConnector extends DESConnect with ServicesConfig {
  lazy val desStubUrl: String = baseUrl("des-stub")
  lazy val desStubURI: String = getConfString("des-stub.uri", "")

  lazy val urlHeaderEnvironment: String = getConfString("des-service.environment", throw new Exception("could not find config value for des-service.environment"))
  lazy val urlHeaderAuthorization: String = s"Bearer ${getConfString("des-service.authorization-token",
    throw new Exception("could not find config value for des-service.authorization-token"))}"

  val http : CorePost = WSHttp
  val auditConnector = MicroserviceAuditConnector
}

trait DESConnect extends HttpErrorFunctions {

  val desStubUrl: String
  val desStubURI: String

  val urlHeaderEnvironment: String
  val urlHeaderAuthorization: String

  val http: CorePost

  def submitToDES(submission: DESSubmission, regId: String)(implicit hc: HeaderCarrier): Future[HttpResponse] = {
    val url = s"$desStubUrl/$desStubURI"
    vatPOST[DESSubmission, HttpResponse](url, submission) map { resp =>
      resp
    }
  }

  @inline
  private def vatPOST[I, O](url: String, body: I, headers: Seq[(String, String)] = Seq.empty)(implicit wts: Writes[I], rds: HttpReads[O], hc: HeaderCarrier, ec: ExecutionContext) =
    http.POST[I, O](url, body, headers)(wts = wts, rds = rds, hc = createHeaderCarrier(hc), ec = ec)

  private def createHeaderCarrier(headerCarrier: HeaderCarrier): HeaderCarrier = {
    headerCarrier.
      withExtraHeaders("Environment" -> urlHeaderEnvironment).
      copy(authorization = Some(Authorization(urlHeaderAuthorization)))
  }

}

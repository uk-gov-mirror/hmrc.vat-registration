/*
 * Copyright 2021 HM Revenue & Customs
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

import config.BackendConfig
import httpparsers.VatSubmissionHttpParser.VatSubmissionHttpReads
import javax.inject.{Inject, Singleton}
import play.api.libs.json.JsObject
import uk.gov.hmrc.http.logging.Authorization
import uk.gov.hmrc.http.{HttpClient, _}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class VatSubmissionConnector @Inject()(appConfig: BackendConfig,
                                       http: HttpClient
                                      )(implicit executionContext: ExecutionContext) {

  def submit(submissionData: JsObject, correlationId: String, credentialId: String)(implicit hc: HeaderCarrier): Future[HttpResponse] = {

    val updatedHeaderCarrier: HeaderCarrier =
      hc.copy(authorization = Some(Authorization(appConfig.urlHeaderAuthorization)))
        .withExtraHeaders(
          "Environment" -> appConfig.urlHeaderEnvironment,
          "CorrelationId" -> correlationId,
          "Credential-Id" -> credentialId
        )

    http.POST[JsObject, HttpResponse](
      url = appConfig.vatSubmissionUrl,
      body = submissionData
    )(
      wts = JsObject.writes,
      rds = VatSubmissionHttpReads,
      hc = updatedHeaderCarrier,
      ec = executionContext
    )

  }

}

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
import models.api.VatSubmission
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.logging.Authorization
import uk.gov.hmrc.http.HttpClient

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class VatSubmissionConnector @Inject()(config: BackendConfig,
                                       http: HttpClient
                                      )(implicit executionContext: ExecutionContext) {

  def submit(submissionData: VatSubmission, correlationId: String, credentialId: String)(implicit hc: HeaderCarrier): Future[HttpResponse] = {

    val updatedHeaderCarrier =
      hc.copy(authorization = Some(Authorization(config.urlHeaderAuthorization)))
        .withExtraHeaders(
          "Environment" -> config.urlHeaderEnvironment,
          "CorrelationId" -> correlationId,
          "Credential-Id" -> credentialId
        )

    http.POST[VatSubmission, HttpResponse](config.vatSubmissionUrl, submissionData)(
      VatSubmission.submissionFormat,
      VatSubmissionHttpReads,
      updatedHeaderCarrier,
      executionContext
    )

  }

}

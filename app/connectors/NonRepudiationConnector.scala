/*
 * Copyright 2020 HM Revenue & Customs
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
import javax.inject.{Inject, Singleton}
import models.nonrepudiation.{NonRepudiationMetadata, NonRepudiationSubmissionAccepted}
import play.api.http.Status.ACCEPTED
import play.api.libs.json.Json
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException}
import uk.gov.hmrc.play.bootstrap.http.HttpClient

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class NonRepudiationConnector @Inject()(httpClient: HttpClient, config: BackendConfig)(implicit ec: ExecutionContext) {
  def submitNonRepudiation(encodedPayloadString: String,
                           nonRepudiationMetadata: NonRepudiationMetadata
                          )(implicit hc: HeaderCarrier): Future[NonRepudiationSubmissionAccepted] = {
    val jsonBody = Json.obj(
      "payload" -> encodedPayloadString,
      "metadata" -> nonRepudiationMetadata
    )

    httpClient.POST(
      url = config.nonRepudiationSubmissionUrl,
      body = jsonBody,
      headers = Seq("X-API-Key" -> config.nonRepudiationApiKey)
    ).map {
      response =>
        response.status match {
          case ACCEPTED =>
            val submissionId = (response.json \ "nrSubmissionId").as[String]
            NonRepudiationSubmissionAccepted(submissionId)
          case _ =>
            throw new InternalServerException("invalid response from NRS") //TODO - handle different errors elegantly
        }
    }
  }
}

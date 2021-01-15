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

package services.submission

import connectors.VatSubmissionConnector
import models.api.Submitted
import uk.gov.hmrc.http.HeaderCarrier
import utils.IdGenerator

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class SubmissionService @Inject()(vatSubmissionConnector: VatSubmissionConnector,
                                  submissionPayloadBuilder: SubmissionPayloadBuilder,
                                  idGenerator: IdGenerator
                                 )(implicit ec: ExecutionContext) {

 def submit(regId: String)(implicit hc: HeaderCarrier) = {
//    for {
//      correlationId <- idGenerator.createId
//      submission <- submissionPayloadBuilder.buildSubmissionPayload(regId)
//      _ <- vatSubmissionConnector.submit(submission, correlationId, credentialId)
//    } yield Submitted
// TODO complete when all other blocks are completed
 }

}

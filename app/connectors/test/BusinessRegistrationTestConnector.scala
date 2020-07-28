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

package connectors.test

import config.BackendConfig
import javax.inject.{Inject, Singleton}
import models.external.BusinessRegistrationRequest
import play.api.libs.json.Json
import play.api.mvc.{Result, Results}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.http.HttpClient

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class BusinessRegistrationTestConnector @Inject()(val backendConfig: BackendConfig, val http: HttpClient) {
  val businessRegUrl: String = backendConfig.servicesConfig.baseUrl("business-registration")

  def createCurrentProfileEntry()(implicit hc: HeaderCarrier): Future[Result] = {
    http.POST(s"$businessRegUrl/business-registration/business-tax-registration", Json.toJson(BusinessRegistrationRequest("ENG"))).map(_ => Results.Ok)
  }
}

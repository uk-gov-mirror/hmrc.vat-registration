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

package connectors.test

import com.google.inject.ImplementedBy
import config.WSHttp
import models.external.BusinessRegistrationRequest
import play.api.libs.json.Json
import play.api.mvc.{Result, Results}
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@ImplementedBy(classOf[VatRegBusinessRegistrationTestConnector])
trait BusinessRegistrationTestConnector {
  def createCurrentProfileEntry()(implicit hc: HeaderCarrier): Future[Result]
}


class VatRegBusinessRegistrationTestConnector extends BusinessRegistrationTestConnector with ServicesConfig {

  //$COVERAGE-OFF$
  val businessRegUrl = baseUrl("business-registration")
  val http = WSHttp

  def createCurrentProfileEntry()(implicit hc: HeaderCarrier): Future[Result] =
    http.POST(s"$businessRegUrl/business-registration/business-tax-registration",
      Json.toJson(BusinessRegistrationRequest("ENG"))).map(_ => Results.Ok)

  //$COVERAGE-ON$

}


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

import common.exceptions._
import config.WSHttp
import models.external.CurrentProfile
import play.api.Logger
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


class VatRegBusinessRegistrationConnector extends BusinessRegistrationConnector with ServicesConfig {
  //$COVERAGE-OFF$
  val businessRegUrl = baseUrl("business-registration")
  val http = WSHttp
  //$COVERAGE-ON$
}


trait BusinessRegistrationConnector {

  val businessRegUrl: String
  val http: HttpGet with HttpPost
  val logPrefix: String = "[BusinessRegistrationConnector] [retrieveCurrentProfile]"

  def retrieveCurrentProfile(implicit hc: HeaderCarrier, rds: HttpReads[CurrentProfile]): Future[Either[LeftState, CurrentProfile]] = {

    http.GET[CurrentProfile](s"$businessRegUrl/business-registration/business-tax-registration").map(Right(_))
      .recover {
        case e: NotFoundException =>
          Logger.error(s"$logPrefix - Received a NotFound status code when expecting current profile from Business-Registration")
          Left(NotFound)
        case e: ForbiddenException =>
          Logger.error(s"$logPrefix - Received a Forbidden status code when expecting current profile from Business-Registration")
          Left(Forbidden)
        case e: Exception =>
          Logger.error(s"$logPrefix - Received error when expecting current profile from Business-Registration - Error ${e.getMessage}")
          Left(GenericError(e))
      }
  }
}

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

import common.exceptions._
import config.BackendConfig
import javax.inject.{Inject, Singleton}
import models.external.CurrentProfile
import org.slf4j.{Logger, LoggerFactory}
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.bootstrap.http.HttpClient

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class BusinessRegistrationConnector @Inject()(val backendConfig: BackendConfig, val http: HttpClient){
  lazy val businessRegUrl: String = backendConfig.servicesConfig.baseUrl("business-registration")

  private val logger: Logger = LoggerFactory.getLogger(getClass)

  def retrieveCurrentProfile(implicit hc: HeaderCarrier, rds: HttpReads[CurrentProfile]): Future[Either[LeftState, CurrentProfile]] = {
    http.GET[CurrentProfile](s"$businessRegUrl/business-registration/business-tax-registration").map(Right(_)).recover {
      case e: NotFoundException =>
        logger.error("Received a NotFound status code when expecting current profile from Business-Registration")
        Left(ResourceNotFound(e.message))
      case e: ForbiddenException =>
        logger.error("Received a Forbidden status code when expecting current profile from Business-Registration")
        Left(ForbiddenAccess(e.message))
      case e: Exception =>
        logger.error(s"Received error when expecting current profile from Business-Registration - Error ${e.getMessage}")
        Left(GenericError(e))
    }
  }
}

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

package auth

import connectors.{AuthConnector, Authority}
import org.slf4j.{Logger, LoggerFactory}
import play.api.mvc.Result
import play.api.mvc.Results._

import scala.concurrent.Future
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._

trait Authenticated {

  val auth: AuthConnector

  private val logger: Logger = LoggerFactory.getLogger(getClass)

  def authenticated(f: Authority => Future[Result])(implicit hc: HeaderCarrier): Future[Result] = {
    logger.debug(s"Current user id is ${hc.userId}")
    auth.getCurrentAuthority() flatMap {
      case Some(authority) =>
        logger.debug(s"Got authority = $authority")
        f(authority)
      case None => Future.successful(Forbidden)
    }
  }

}

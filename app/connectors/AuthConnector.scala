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

import config.WSHttp
import org.slf4j.{Logger, LoggerFactory}
import play.api.http.Status._
import play.api.libs.json.Json
import uk.gov.hmrc.play.config.ServicesConfig

import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._
import scala.concurrent.Future
import uk.gov.hmrc.http._

case class UserIds(internalId: String, externalId: String)

object UserIds {
  implicit val format = Json.format[UserIds]
}

case class Authority(uri: String, gatewayId: String, userDetailsLink: String, ids: UserIds)

object Authority {
  implicit val format = Json.format[Authority]
}

class VatRegAuthConnector extends AuthConnector {
  lazy val serviceUrl = baseUrl("auth")
  val authorityUri    = "auth/authority"
  val http: CoreGet   = WSHttp
}

trait AuthConnector extends ServicesConfig with RawResponseReads {

  val serviceUrl: String
  val authorityUri: String
  val http: CoreGet

  private val logger: Logger = LoggerFactory.getLogger(getClass)

  def getCurrentAuthority()(implicit headerCarrier: HeaderCarrier): Future[Option[Authority]] = {
    val getUrl = s"""$serviceUrl/$authorityUri"""
    logger.debug(s"[getCurrentAuthority] - GET $getUrl")
    http.GET[HttpResponse](getUrl) flatMap { response =>
      logger.debug(s"[getCurrentAuthority] - RESPONSE status: ${response.status}, body: ${response.body}")
      response.status match {
        case OK =>
          val uri         = (response.json \ "uri").as[String]
          val gatewayId   = (response.json \ "credentials" \ "gatewayId").as[String]
          val userDetails = (response.json \ "userDetailsLink").as[String]
          val idsLink     = (response.json \ "ids").as[String]

          http.GET[HttpResponse](s"$serviceUrl$idsLink") map { response =>
            val ids = response.json.as[UserIds]
            Some(Authority(uri, gatewayId, userDetails, ids))
          }
        case _ => Future.successful(None)
      }
    }
  }
}

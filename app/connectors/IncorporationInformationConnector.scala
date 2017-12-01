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

import common.{RegistrationId, TransactionId}
import config.WSHttp
import models.external.IncorporationStatus
import play.api.Logger
import play.api.http.Status.{ACCEPTED, OK}
import play.api.libs.json._
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._

import scala.concurrent.Future
import scala.util.control.NoStackTrace

class IncorporationInformationResponseException(msg: String) extends NoStackTrace {
  override def getMessage: String = msg
}

class VatRegIncorporationInformationConnector extends IncorporationInformationConnector with ServicesConfig {
  //$COVERAGE-OFF$
  lazy val iiUrl                  = baseUrl("incorporation-information")
  lazy val iiUri                  = getConfString("incorporation-information.uri", "")
  lazy val vatRegUri: String      = baseUrl("vat-registration")
  val http: CoreGet with CorePost = WSHttp
  //$COVERAGE-ON$
}

case class IncorpStatusRequest(callbackUrl: String)

object IncorpStatusRequest {
  implicit val writes: Writes[IncorpStatusRequest] = Writes(
    isr => Json.obj(
      "SCRSIncorpSubscription" -> Json.obj(
        "callbackUrl" -> s"${isr.callbackUrl}/vat-registration/incorporation-data"
      )
    )
  )
}

trait IncorporationInformationConnector {
  val iiUrl : String
  val iiUri : String
  val http: CoreGet with CorePost
  val vatRegUri : String

  private[connectors] def constructIncorporationInfoUri(transactionId: TransactionId, regime: String, subscriber: String): String = {
    s"$iiUri/subscribe/$transactionId/regime/$regime/subscriber/$subscriber"
  }

  def retrieveIncorporationStatus(transactionId: TransactionId, regime : String, subscriber : String)
                                 (implicit hc: HeaderCarrier, rds: HttpReads[IncorporationStatus]): Future[Option[IncorporationStatus]] = {
    http.POST[IncorpStatusRequest, HttpResponse](s"$iiUrl${constructIncorporationInfoUri(transactionId, regime, subscriber)}",
      IncorpStatusRequest(vatRegUri)) map { resp => {
        resp.status match {
          case OK         => Some(resp.json.as[IncorporationStatus](IncorporationStatus.iiReads))
          case ACCEPTED   => None
          case _          =>
            Logger.error(s"[IncorporationInformationConnector] - [getIncorporationUpdate] returned a ${resp.status} response code for txId: $transactionId")
            throw new IncorporationInformationResponseException(s"Calling II on ${constructIncorporationInfoUri(transactionId, regime, subscriber)} returned a ${resp.status}")
        }
      }
    }
  }

  def getCompanyName(regId: RegistrationId, transactionId: TransactionId)(implicit hc: HeaderCarrier): Future[HttpResponse] = {
    http.GET[HttpResponse](s"$iiUrl$iiUri/$transactionId/company-profile") recover {
      case notFound: NotFoundException =>
        Logger.error(s"[IncorporationInformationConnector] - [getCompanyName] - Could not find company name for regId $regId (txId: $transactionId)")
        throw notFound
      case e =>
        Logger.error(s"[IncorporationInformationConnector] - [getCompanyName] - There was a problem getting company for regId $regId (txId: $transactionId)", e)
        throw e
    }
  }
}

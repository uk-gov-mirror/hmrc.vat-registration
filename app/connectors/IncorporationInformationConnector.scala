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

import common.{RegistrationId, TransactionId}
import config.BackendConfig
import javax.inject.Inject
import models.external.IncorporationStatus
import org.joda.time.LocalDateTime
import org.joda.time.format.DateTimeFormat
import play.api.Logger
import play.api.http.Status.{ACCEPTED, OK}
import play.api.libs.json._
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import scala.concurrent.ExecutionContext.Implicits.global

import scala.concurrent.Future
import scala.util.control.NoStackTrace

class IncorporationInformationResponseException(msg: String) extends NoStackTrace {
  override def getMessage: String = msg
}

class VatRegIncorporationInformationConnector @Inject()(val backendConfig: BackendConfig, val http: HttpClient) extends IncorporationInformationConnector {
  //$COVERAGE-OFF$
  lazy val iiUrl                  = backendConfig.baseUrl("incorporation-information")
  lazy val iiUri                  = backendConfig.getConfString("incorporation-information.uri", "")
  lazy val vatRegUri: String      = backendConfig.baseUrl("vat-registration")
  //$COVERAGE-ON$
}

case class IncorpStatusRequest(callbackUrl: String)

object IncorpStatusRequest {
  implicit val writes: Writes[IncorpStatusRequest] = Writes(
    isr => Json.obj(
      "SCRSIncorpSubscription" -> Json.obj(
        "callbackUrl" -> s"${isr.callbackUrl}/vatreg/incorporation-data"
      )
    )
  )
}

trait IncorporationInformationConnector {
  val iiUrl: String
  val iiUri: String
  val http: CoreGet with CorePost
  val vatRegUri: String

  private[connectors] def constructIncorporationInfoUri(transactionId: TransactionId, regime: String, subscriber: String): String = {
    s"$iiUri/subscribe/$transactionId/regime/$regime/subscriber/$subscriber"
  }

  def retrieveIncorporationStatus(regId: Option[String], transactionId: TransactionId, regime: String, subscriber: String)
                                 (implicit hc: HeaderCarrier, rds: HttpReads[IncorporationStatus]): Future[Option[IncorporationStatus]] = {
    val dtNow = DateTimeFormat.forPattern("dd/MM/yyyy HH:mm:ss").print(LocalDateTime.now)
    val registrationId = regId.fold("")(reg => s" registration Id: $reg")

    http.POST[IncorpStatusRequest, HttpResponse](s"$iiUrl${constructIncorporationInfoUri(transactionId, regime, subscriber)}",
      IncorpStatusRequest(vatRegUri)) map { resp =>
      resp.status match {
        case OK =>         Some(resp.json.as[IncorporationStatus](IncorporationStatus.iiReads))
        case ACCEPTED =>   None
        case _ =>
           Logger.info(s"[IncorporationInformationConnector] - [getIncorporationUpdate]" +
           s" returned a ${resp.status} response code for txId: $transactionId")
           Logger.error(s"FAILED_VAT_SUBSCRIPTION_TO_II : Time occurred was $dtNow$registrationId")
           throw new IncorporationInformationResponseException(
             s"Calling II on ${constructIncorporationInfoUri(transactionId, regime, subscriber)} returned a ${resp.status}"
           )
      }
    } recover {
      case e => Logger.info(s"[IncorporationInformationConnector] - [getIncorporationUpdate] returned a ${e} exception for txId: $transactionId")
        Logger.error(s"FAILED_VAT_SUBSCRIPTION_TO_II : Time occurred was $dtNow$registrationId")
        throw new IncorporationInformationResponseException(
          s"Calling II on ${constructIncorporationInfoUri(transactionId, regime, subscriber)} returned a ${e} exception"
        )
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

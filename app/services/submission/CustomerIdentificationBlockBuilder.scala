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

import play.api.libs.json.{JsObject, Json}
import repositories.RegistrationMongoRepository
import services.submission.JsonUtils.{jsonObject, optional}
import uk.gov.hmrc.http.InternalServerException

import scala.concurrent.{ExecutionContext, Future}

class CustomerIdentificationBlockBuilder(registrationMongoRepository: RegistrationMongoRepository)(implicit ec: ExecutionContext) {

  def buildCustomerIdentificationBlock(regId: String): Future[JsObject] = for {
    optApplicantDetails <- registrationMongoRepository.getApplicantDetails(regId)
    optTradingDetails <- registrationMongoRepository.retrieveTradingDetails(regId)
  } yield (optApplicantDetails, optTradingDetails) match {
    case (Some(applicantDetails), Some(tradingDetails)) =>
      jsonObject(
        "tradersPartyType" -> "50",
        "shortOrgName" -> applicantDetails.companyName,
        optional("tradingName" -> tradingDetails.tradingName)
      ) ++ {
        (applicantDetails.bpSafeId, applicantDetails.companyNumber, applicantDetails.ctutr) match {
          case (Some(bpSafeId), _, _) =>
            Json.obj("primeBPSafeID" -> bpSafeId)
          case (None, Some(companyNumber), Some(ctutr)) =>
            Json.obj("customerID" ->
              Json.arr(
                jsonObject(
                  "idType" -> "UTR",
                  optional("IDsVerificationStatus" -> applicantDetails.idVerificationStatus),
                  "idValue" -> ctutr
                ),
                jsonObject(
                  "idType" -> "CRN",
                  "idValue" -> companyNumber,
                  "date" -> applicantDetails.dateOfIncorporation,
                  optional("IDsVerificationStatus" -> applicantDetails.idVerificationStatus)
                )
              )
            )
          case _ =>
            throw new InternalServerException("Could not build customer identification block for submission due to missing data")
        }
      }
    case _ =>
      throw new InternalServerException("Could not build customer identification block for submission due to missing data")
  }

}

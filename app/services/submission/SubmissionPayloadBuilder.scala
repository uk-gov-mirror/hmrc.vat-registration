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

import models.api.EligibilitySubmissionData.voluntaryKey
import models.api.{ContactPreference, Email, Letter}
import play.api.libs.json.{JsObject, JsValue, Json, Writes}
import repositories.RegistrationMongoRepository
import services.submission.JsonUtils.{optional, _}
import uk.gov.hmrc.http.InternalServerException

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SubmissionPayloadBuilder @Inject()(registrationMongoRepository: RegistrationMongoRepository,
                                         periodsBlockBuilder: PeriodsBlockBuilder
                                        )(implicit ec: ExecutionContext) {

  def buildSubmissionPayload(regId: String): Future[JsObject] = for {
    adminBlock <- buildAdminBlock(regId)
    customerIdentificationBlock <- buildCustomerIdentificationBlock(regId)
    contactBlock <- buildContactBlock(regId)
    subscriptionBlock <- buildSubscriptionBlock(regId)
    periodsBlock <- periodsBlockBuilder.buildPeriodsBlock(regId)
  } yield jsonObject(
    "admin" -> adminBlock,
    "customerIdentification" -> customerIdentificationBlock,
    "contact" -> contactBlock,
    "subscription" -> subscriptionBlock,
    "periods" -> periodsBlock
  )

  private def buildAdminBlock(regId: String): Future[JsObject] = for {
    optEligibilityData <- registrationMongoRepository.fetchEligibilitySubmissionData(regId)
    optTradingDetails <- registrationMongoRepository.retrieveTradingDetails(regId)
  } yield (optEligibilityData, optTradingDetails) match {
    case (Some(eligibilityData), Some(tradingDetails)) =>
      Json.obj(
        "additionalInformation" -> Json.obj(
          "customerStatus" -> eligibilityData.customerStatus
        ),
        "attachments" -> Json.obj(
          "EORIrequested" -> tradingDetails.eoriRequested
        )
      )
    case _ =>
      throw new InternalServerException("Could not build admin block for submission due to missing data")
  }

  private def buildCustomerIdentificationBlock(regId: String): Future[JsObject] = for {
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
                  "IDsVerificationStatus" -> "3",
                  "idValue" -> ctutr
                ),
                jsonObject(
                  "idType" -> "CRN",
                  "idValue" -> companyNumber,
                  "date" -> applicantDetails.dateOfIncorporation,
                  "IDsVerificationStatus" -> "3"
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

  private def buildContactBlock(regId: String): Future[JsObject] = for {
    optBusinessContact <- registrationMongoRepository.fetchBusinessContact(regId)
  } yield optBusinessContact match {
    case Some(businessContact) =>
      Json.obj(
        "address" -> jsonObject(
          "line1" -> businessContact.ppob.line1,
          "line2" -> businessContact.ppob.line2,
          optional("line3" -> businessContact.ppob.line3),
          optional("line4" -> businessContact.ppob.line4),
          optional("postCode" -> businessContact.ppob.postcode),
          optional("countryCode" -> businessContact.ppob.country.flatMap(_.code)),
          optional("addressValidated" -> businessContact.ppob.addressValidated)
        ),
        "commDetails" -> jsonObject(
          optional("telephone" -> businessContact.digitalContact.tel),
          optional("mobileNumber" -> businessContact.digitalContact.mobile),
          "email" -> businessContact.digitalContact.email,
          "commsPreference" -> (businessContact.commsPreference match {
            case Email => ContactPreference.electronic
            case Letter => ContactPreference.paper
          })
        )
      )
    case _ =>
      throw new InternalServerException("Could not build contact block for submission due to missing data")
  }

  private def buildSubscriptionBlock(regId: String): Future[JsObject] = for {
    optEligibilityData <- registrationMongoRepository.fetchEligibilitySubmissionData(regId)
    optReturns <- registrationMongoRepository.fetchReturns(regId)
    optApplicantDetails <- registrationMongoRepository.getApplicantDetails(regId)
    optSicAndCompliance <- registrationMongoRepository.fetchSicAndCompliance(regId)
  } yield (optEligibilityData, optReturns, optApplicantDetails, optSicAndCompliance) match {
    case (Some(eligibilityData), Some(returns), Some(applicantDetails), Some(sicAndCompliance)) => Json.obj(
      "reasonForSubscription" -> jsonObject(
        "registrationReason" -> eligibilityData.reasonForRegistration,
        optional("relevantDate" -> {
          if (eligibilityData.reasonForRegistration != voluntaryKey) {
            Some(eligibilityData.earliestDate)
          } else {
            returns.start.date
          }
        }),
        optional("voluntaryOrEarlierDate" -> returns.start.date),
        "exemptionOrException" -> eligibilityData.exceptionOrExemption
      ),
      "corporateBodyRegistered" -> jsonObject(
        optional("companyRegistrationNumber" -> applicantDetails.companyNumber),
        "dateOfIncorporation" -> applicantDetails.dateOfIncorporation,
        "countryOfIncorporation" -> applicantDetails.countryOfIncorporation
      ),
      "businessActivities" -> Json.obj(
        "description" -> sicAndCompliance.businessDescription,
        "SICCodes" -> Json.obj(
          "primaryMainCode" -> sicAndCompliance.mainBusinessActivity.id
        )
      ),
      "yourTurnover" -> Json.obj(
        "turnoverNext12Months" -> "",
        "zeroRatedSupplies" -> 12.99,
        "VATRepaymentExpected" -> true
      ),
      "schemes" -> Json.obj(
        "FRSCategory" -> "",
        "FRSPercentage" -> "",
        "startDate" -> "",
        "limitedCostTrader" -> ""
      )
    )
    case _ =>
      throw new InternalServerException("Could not build subscription block for submission due to missing data")
  }

}

object JsonUtils {

  case class JsonField private(json: Option[(String, JsValue)])

  def jsonObject(fields: JsonField*): JsObject =
    JsObject(fields.flatMap(_.json))

  implicit def toJsonField[T](field: (String, T))(implicit writer: Writes[T]): JsonField =
    JsonField(Some(field._1 -> writer.writes(field._2)))

  def optional[T](field: (String, Option[T]))(implicit writer: Writes[T]): JsonField =
    field match {
      case (key, Some(value)) =>
        JsonField(Some(field._1 -> writer.writes(value)))
      case (key, None) =>
        JsonField(None)
    }
}


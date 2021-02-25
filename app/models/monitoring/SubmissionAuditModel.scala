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

package models.monitoring

import models.api.{ApplicantDetails, CustomerStatus, VatScheme}
import play.api.libs.json.{JsValue, Json}
import services.monitoring.AuditModel
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.http.InternalServerException
import utils.JsonUtils._

case class SubmissionAuditModel(detailBlock: JsValue,
                                vatScheme: VatScheme,
                                authProviderId: String,
                                affinityGroup: AffinityGroup,
                                optAgentReferenceNumber: Option[String]) extends AuditModel {

  private def customerStatus: CustomerStatus = vatScheme.eligibilitySubmissionData
    .map(_.customerStatus)
    .getOrElse(throw new InternalServerException("Customer status missing from Eligibility data"))

  private def eoriRequested: Boolean = vatScheme.tradingDetails
    .map(_.eoriRequested)
    .getOrElse(throw new InternalServerException("EORI requested answer missing from Trading details"))

  private def applicantDetails: ApplicantDetails = vatScheme.applicantDetails
    .getOrElse(throw new InternalServerException("Applicant details section missing"))

  override val auditType: String = "_"
  override val transactionName: String = "_"
  override val detail: JsValue = jsonObject(
    "authProviderId" -> authProviderId,
    "journeyId" -> vatScheme.id,
    "userType" -> affinityGroup.toString,
    optional("agentReferenceNumber" -> optAgentReferenceNumber.filterNot(_ == "")),
    "messageType" -> "SubscriptionCreate",
    "customerStatus" -> customerStatus,
    "eoriRequested" -> eoriRequested,
    "corporateBodyRegistered" -> Json.obj(
      "dateOfIncorporation" -> applicantDetails.dateOfIncorporation,
      "countryOfIncorporation" -> applicantDetails.countryOfIncorporation
    ),
    "idsVerificationStatus" -> "1",
    "cidVerification" -> "1",
    optional("businessPartnerReference" -> applicantDetails.bpSafeId),
    "detail" -> detailBlock
  )

}

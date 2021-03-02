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

package services.monitoring

import models.api.VatScheme
import models.submission.UkCompany
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.http.InternalServerException
import utils.JsonUtils._

import javax.inject.Singleton

@Singleton
class CustomerIdentificationBlockBuilder extends {

  def buildCustomerIdentificationBlock(vatScheme: VatScheme): JsValue = {
    (vatScheme.applicantDetails, vatScheme.tradingDetails) match {
      case (Some(applicantDetails), Some(tradingDetails)) =>
        jsonObject(
          "tradersPartyType" -> UkCompany.toString, // TODO: refactor once we allow different entities
          "identifiers" -> Json.obj(
            "companyRegistrationNumber" -> applicantDetails.companyNumber,
            "ctUTR" -> applicantDetails.ctutr
          ),
          "shortOrgName" -> applicantDetails.companyName,
          "dateOfBirth" -> applicantDetails.dateOfBirth,
          optional("tradingName" -> tradingDetails.tradingName)
        )
      case (None, _) =>
        throw new InternalServerException("[CustomerIdentificationBlockBuilder][Audit] Could not build customerIdentification block due to missing Applicant details data")
      case (_, None) =>
        throw new InternalServerException("[CustomerIdentificationBlockBuilder][Audit] Could not build customerIdentification block due to missing Trading details data")
    }
  }


}

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

package helpers

import models.api.{ApplicantDetails, BvFail, BvPass, BvUnchallenged}
import models.submission._
import play.api.libs.json.JsValue

trait ApplicantDetailsHelper {

  implicit class ApplicantDetailsWriter(applicantDetails: ApplicantDetails) {
    val businessVerificationStatus = applicantDetails.businessVerification collect {
      case BvPass => IdVerified
      case BvFail => IdVerificationFailed
      case BvUnchallenged => IdUnverifiable
    }

    def personalIdentifiers: List[CustomerId] = List(
      CustomerId(applicantDetails.nino, NinoIdType, Some(IdVerified), date = Some(applicantDetails.dateOfBirth.date))
    )

    def companyIdentifiers: List[CustomerId] = List(
      applicantDetails.ctutr.map(utr =>
        CustomerId(
          idValue = utr,
          idType = UtrIdType,
          IDsVerificationStatus = businessVerificationStatus
        )
      ),
      applicantDetails.companyNumber.map(crn =>
        CustomerId(
          idValue = crn,
          idType = CrnIdType,
          IDsVerificationStatus = businessVerificationStatus,
          date = Some(applicantDetails.dateOfIncorporation)
        )
      )
    ).flatten
  }

  implicit class ApplicantDetailsReader(json: JsValue) {
    private def jsonException(field: String): Exception = new Exception(s"Couldn't read $field from ApplicantDetails JSON")

    private def getOptionalId(ids: List[CustomerId], idType: IdType): Option[String] =
      ids.find(_.idType == idType)
        .map(_.idValue)

    private def getId(ids: List[CustomerId], idType: IdType): String =
      getOptionalId(ids, idType)
        .getOrElse(throw jsonException(idType.toString))

    val customerIds: List[CustomerId] = (json \ "customerIdentification" \ "customerID")
      .asOpt[List[CustomerId]]
      .getOrElse(List())

    val personalIds : List[CustomerId] = (json \ "declaration" \ "applicantDetails" \ "identifiers").as[List[CustomerId]]
    val crn: Option[String] = getOptionalId(customerIds, CrnIdType)
    val ctUtr: Option[String] = getOptionalId(customerIds, UtrIdType)
    val nino: String = getId(personalIds, NinoIdType)

    val businessVerificationStatus =
      (for {
        ids <- customerIds.find(_.idType == CrnIdType)
        status <- ids.IDsVerificationStatus
      } yield status) collect {
        case IdVerified => BvPass
        case IdUnverifiable => BvUnchallenged
        case IdVerificationFailed => BvFail
      }
  }

}

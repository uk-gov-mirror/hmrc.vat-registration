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

package helpers

import models.api._
import models.submission._
import uk.gov.hmrc.http.InternalServerException

trait ApplicantDetailsHelper {

  implicit class ApplicantDetailsWriter(applicantDetails: ApplicantDetails) {

    def idVerificationStatus: IdVerificationStatus =
      (applicantDetails.identifiersMatch, applicantDetails.businessVerification, applicantDetails.registration) match {
        case (true, BvPass, FailedStatus) => IdVerified
        case (true, BvCtEnrolled, FailedStatus) => IdVerified
        case (true, BvFail, NotCalledStatus) => IdVerificationFailed
        case (true, BvUnchallenged, NotCalledStatus) => IdVerificationFailed
        case (false, BvUnchallenged, NotCalledStatus) => IdUnverifiable
        case _ => throw new InternalServerException("[ApplicantDetailsHelper][idVerificationStatus] method called with unsupported data from incorpId")
      }

    def personalIdentifiers: List[CustomerId] = List(
      CustomerId(applicantDetails.nino, NinoIdType, IdVerified, date = Some(applicantDetails.dateOfBirth.date))
    )

    def companyIdentifiers: List[CustomerId] = List(
      CustomerId(
        idValue = applicantDetails.ctutr,
        idType = UtrIdType,
        IDsVerificationStatus = idVerificationStatus
      ),
      CustomerId(
        idValue = applicantDetails.companyNumber,
        idType = CrnIdType,
        IDsVerificationStatus = idVerificationStatus,
        date = Some(applicantDetails.dateOfIncorporation)
      )
    )
  }

}

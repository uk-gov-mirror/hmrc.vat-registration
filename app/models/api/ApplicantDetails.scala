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

package models.api

import models.submission.{CustomerId, DateOfBirth, NinoIdType}
import play.api.libs.functional.syntax._
import play.api.libs.json._

case class ApplicantDetails(nino: String,
                            role: Option[String] = None,
                            name: Name,
                            dateOfBirth: DateOfBirth,
                            currentAddress: Address,
                            contact: DigitalContactOptional,
                            changeOfName: Option[FormerName] = None,
                            previousAddress : Option[Address] = None)

object ApplicantDetails extends VatApplicantDetailsValidator {

  implicit val format: Format[ApplicantDetails] = (
    (__ \ "nino").format[String] and
    (__ \ "role").formatNullable[String] and
    (__ \ "name").format[Name] and
    (__ \ "dateOfBirth").format[DateOfBirth] and
    (__ \ "currentAddress").format[Address] and
    (__ \ "contact").format[DigitalContactOptional] and
    (__ \ "changeOfName").formatNullable[FormerName] and
    (__ \ "previousAddress").formatNullable[Address]
  )(ApplicantDetails.apply, unlift(ApplicantDetails.unapply))

  val submissionFormat: Format[ApplicantDetails] = (
    (__ \ "customerIdentification" \ "customerID").format[CustomerId].inmap[String](_.idValue, nino => CustomerId(nino, NinoIdType)) and
    (__ \ "declaration" \ "applicantDetails" \ "roleInBusiness").formatNullable[String] and
    (__).format[Name](Name.submissionFormat) and
    (__).format[DateOfBirth](DateOfBirth.submissionFormat) and
    (__ \ "declaration" \ "applicantDetails" \ "currAddress").format[Address](Address.submissionFormat) and
    (__ \ "declaration" \ "applicantDetails" \ "commDetails").format[DigitalContactOptional](DigitalContactOptional.submissionFormat) and
    (__ \ "declaration" \ "applicantDetails" \ "prevName").formatNullable[FormerName](FormerName.submissionFormat) and
    (__ \ "declaration" \ "applicantDetails" \ "prevAddress").formatNullable[Address](Address.submissionFormat)
  )(ApplicantDetails.apply, unlift(ApplicantDetails.unapply))

}

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

package models.api

import auth.Crypto
import play.api.libs.functional.syntax._
import play.api.libs.json._

@deprecated("Use BankAccount instead", "11/12/2017")
case class VatBankAccount(accountName: String,
                          accountSortCode: String,
                          accountNumber: String)

@deprecated("Use BankAccount instead", "11/12/2017")
object VatBankAccount extends VatBankAccountValidator {
  implicit val format: OFormat[VatBankAccount] = (
    (__ \ "accountName").format[String] and
    (__ \ "accountSortCode").format[String](accountSortCodeValidator) and
    (__ \ "accountNumber").format[String](accountNumberValidator)
  )(VatBankAccount.apply, unlift(VatBankAccount.unapply))
}
@deprecated("Use BankAccountMongoFormat instead", "11/12/2017")
object VatBankAccountMongoFormat {
  implicit val encryptedFormat: OFormat[VatBankAccount] = (
    (__ \ "accountName").format[String] and
    (__ \ "accountSortCode").format[String] and
    (__ \ "accountNumber").format[String](Crypto.rds)(Crypto.wts)
  )(VatBankAccount.apply, unlift(VatBankAccount.unapply))
}

case class BankAccount(hasBankAccount: Boolean,
                       details: Option[BankAccountDetails])
object BankAccount {
  implicit val format: OFormat[BankAccount] = (
    (__ \ "hasBankAccount").format[Boolean] and
      (__ \ "details").formatNullable[BankAccountDetails]
    )(BankAccount.apply, unlift(BankAccount.unapply))
}



case class BankAccountDetails(name: String,
                              sortCode: String,
                              number: String)

object BankAccountDetails extends VatBankAccountValidator {
  implicit val format: OFormat[BankAccountDetails] = (
    (__ \ "name").format[String] and
      (__ \ "sortCode").format[String](accountSortCodeValidator) and
      (__ \ "number").format[String](accountNumberValidator)
    )(BankAccountDetails.apply, unlift(BankAccountDetails.unapply))
}

object BankAccountDetailsMongoFormat {
  implicit val encryptedFormat: OFormat[BankAccountDetails] = (
    (__ \ "name").format[String] and
      (__ \ "sortCode").format[String] and
      (__ \ "number").format[String](Crypto.rds)(Crypto.wts)
    )(BankAccountDetails.apply, unlift(BankAccountDetails.unapply))
}

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

package models.api

import auth.CryptoSCRS
import play.api.libs.functional.syntax._
import play.api.libs.json._

case class BankAccount(isProvided: Boolean,
                       details: Option[BankAccountDetails])

case class BankAccountDetails(name: String,
                              sortCode: String,
                              number: String)

object BankAccount {
  implicit val format: Format[BankAccount] = Json.format[BankAccount]

  def submissionReads(optJson: Option[JsValue]): Option[BankAccount] = {
    optJson match {
      case Some(json) =>
        val bankNameResult = (json \ "UK" \ "accountName").validate[String]
        val sortCodeResult = (json \ "UK" \ "sortCode").validate[String]
        val accNumberResult = (json \ "UK" \ "accountNumber").validate[String]
        val reasonNoAccount = (json \ "UK" \ "reasonBankAccNotProvided").validate[String]

        (bankNameResult, sortCodeResult, accNumberResult) match {
          case (JsSuccess(name, _), JsSuccess(sortCode, _), JsSuccess(accNumber, _)) =>
            Some(BankAccount(isProvided = true, details = Some(BankAccountDetails(name, sortCode, accNumber))))
          case _ if reasonNoAccount.isSuccess =>
            Some(BankAccount(isProvided = false, details = None))
          case _ =>
            throw new Exception("Could not parse bank details")
        }
      case None =>
        Some(BankAccount(isProvided = false, details = None))
    }

  }

  def submissionWrites: Writes[Option[BankAccount]] = Writes { optBankAccount: Option[BankAccount] =>
    optBankAccount match {
      case Some(BankAccount(true, Some(details))) =>
        Json.obj("UK" -> Json.obj(
          "accountName" -> details.name,
          "sortCode" -> details.sortCode.replaceAll("-", ""),
          "accountNumber" -> details.number
        ))
      case _ =>
        Json.obj(
          "UK" -> Json.obj(
            "reasonBankAccNotProvided" -> "1"
          )
        )
    }
  }
}

object BankAccountDetails extends VatBankAccountValidator {
  implicit val format: Format[BankAccountDetails] = Json.format[BankAccountDetails]

  val submissionFormat: OFormat[BankAccountDetails] = (
    (__ \ "UK" \ "accountName").format[String] and
      (__ \ "UK" \ "sortCode").format[String] and
      (__ \ "UK" \ "accountNumber").format[String]
    ) (BankAccountDetails.apply, unlift(BankAccountDetails.unapply))

  val submissionReads: Reads[BankAccountDetails] = (
    (__ \ "UK" \ "accountName").read[String] and
      (__ \ "UK" \ "sortCode").read[String] and
      (__ \ "UK" \ "accountNumber").read[String]
    ) (BankAccountDetails.apply _)

}

object BankAccountDetailsMongoFormat extends VatBankAccountValidator {
  def format(crypto: CryptoSCRS): Format[BankAccountDetails] = (
    (__ \ "name").format[String] and
      (__ \ "sortCode").format[String] and
      (__ \ "number").format[String](crypto.rds)(crypto.wts)
    ) (BankAccountDetails.apply, unlift(BankAccountDetails.unapply))
}


object BankAccountMongoFormat extends VatBankAccountValidator {
  def encryptedFormat(crypto: CryptoSCRS): OFormat[BankAccount] = (
    (__ \ "isProvided").format[Boolean] and
      (__ \ "details").formatNullable[BankAccountDetails](BankAccountDetailsMongoFormat.format(crypto))
    ) (BankAccount.apply, unlift(BankAccount.unapply))
}
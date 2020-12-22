
package itutil

import play.api.libs.json.{JsObject, JsValue, Json}

trait ITVatSubmissionFixture extends ITFixtures {

  val vatSubmissionJson: JsValue = Json.parse(
    """
      |{
      |  "messageType": "SubmissionCreate",
      |  "customerIdentification": {
      |    "tradingName": "trading-name",
      |    "tradersPartyType": "50",
      |    "shortOrgName": "testCompanyName",
      |    "customerID": [
      |      {
      |        "idValue": "testCtUtr",
      |        "idType": "UTR",
      |        "IDsVerificationStatus": "1"
      |      },
      |      {
      |        "idValue": "testCrn",
      |        "idType": "CRN",
      |        "IDsVerificationStatus": "1",
      |        "date": "2020-01-02"
      |      }
      |    ],
      |    "name": {
      |      "firstName": "Forename",
      |      "lastName": "Surname"
      |    },
      |    "dateOfBirth": "2018-01-01"
      |  },
      |  "declaration": {
      |    "declarationSigning": {
      |      "declarationCapacity": "01",
      |      "confirmInformationDeclaration": true
      |    },
      |    "applicantDetails": {
      |      "commDetails": {
      |        "email": "skylake@vilikariet.com"
      |      },
      |      "name": {
      |        "firstName": "Forename",
      |        "lastName": "Surname"
      |      },
      |      "dateOfBirth": "2018-01-01",
      |      "roleInBusiness": "secretary",
      |      "identifiers": [
      |        {
      |          "idValue": "AB123456A",
      |          "idType": "NINO",
      |          "IDsVerificationStatus": "1",
      |          "date": "2018-01-01"
      |        }
      |      ],
      |      "prevName": {
      |        "firstName": "Forename",
      |        "lastName": "Surname",
      |        "nameChangeDate": "2018-01-01"
      |      },
      |      "currAddress": {
      |        "line1": "line1",
      |        "line2": "line2",
      |        "postCode": "XX XX",
      |        "countryCode": "UK",
      |        "addressValidated": false
      |      }
      |    }
      |  },
      |  "subscription": {
      |    "corporateBodyRegistered": {
      |      "dateOfIncorporation": "2020-01-02",
      |      "companyRegistrationNumber": "testCrn",
      |      "countryOfIncorporation": "GB"
      |    },
      |    "reasonForSubscription": {
      |      "voluntaryOrEarlierDate": "2018-01-01",
      |      "relevantDate": "2020-10-07",
      |      "registrationReason": "0016",
      |      "exemptionOrException": "0"
      |    },
      |    "yourTurnover": {
      |      "VATRepaymentExpected": false,
      |      "turnoverNext12Months": 123456,
      |      "zeroRatedSupplies": 12.99
      |    },
      |    "schemes": {
      |      "startDate": "2018-01-01",
      |      "FRSCategory": "testCategory",
      |      "FRSPercentage": 15,
      |      "limitedCostTrader": false
      |    },
      |    "businessActivities": {
      |     "SICCodes": {
      |        "mainCode2": "00998",
      |        "primaryMainCode": "12345",
      |        "mainCode3": "00889"
      |     },
      |     "description": "this is my business description"
      |    }
      |  },
      |  "bankDetails": {
      |    "UK": {
      |      "accountName": "Test Bank Account",
      |      "sortCode": "010203",
      |      "accountNumber": "01023456"
      |    }
      |  },
      |  "compliance": {
      |    "numOfWorkersSupplied": 1000,
      |    "intermediaryArrangement": true,
      |    "supplyWorkers": true
      |  },
      |  "contact": {
      |    "commDetails": {
      |      "mobileNumber": "54321",
      |      "webAddress": "www.foo.com",
      |      "telephone": "12345",
      |      "email": "email@email.com",
      |      "commsPreference": "ZEL"
      |    },
      |    "address": {
      |      "line1": "line1",
      |      "line2": "line2",
      |      "countryCode": "UK"
      |    }
      |  },
      |  "admin": {
      |    "additionalInformation": {
      |      "customerStatus": "2"
      |    },
      |    "attachments": {
      |      "EORIrequested": true
      |    }
      |  },
      |  "periods": {
      |    "customerPreferredPeriodicity": "MA"
      |  }
      |}""".stripMargin)

  val testBusinessDescription = "testBusinessDescription"

  val testSubmissionJson: JsObject = Json.obj(
    "messageType" -> "SubscriptionCreate",
    "admin" -> Json.obj(
      "additionalInformation" -> Json.obj(
        "customerStatus" -> "2"
      ),
      "attachments" -> Json.obj(
        "EORIrequested" -> true
      )
    ),
    "customerIdentification" -> Json.obj(
      "tradersPartyType" -> "50",
      "customerID" -> Json.arr(
        Json.obj(
          "idType" -> "UTR",
          "idValue" -> testCtUtr,
          "IDsVerificationStatus" -> "3"
        ),
        Json.obj(
          "idType" -> "CRN",
          "idValue" -> testCrn,
          "date" -> testDateOfIncorp,
          "IDsVerificationStatus" -> "3"
        )
      ),
      //No prime BP safe ID included as not registered on ETMP
      "shortOrgName" -> testCompanyName,
      //name, dateOfBirth not included as company
      "tradingName" -> testTradingDetails.tradingName.get
    ),
    "contact" -> Json.obj(
      "address" -> Json.obj(
        "line1" -> testAddress.line1,
        "line2" -> testAddress.line2,
        //line5 not supplied by ALF
        "postCode" -> testAddress.postcode,
        "countryCode" -> "GB",
        "addressValidated" -> true //false if manually entered by user
      ),
      "commDetails" -> Json.obj(
        "telephone" -> testContactDetails.tel,
        "mobileNumber" -> testContactDetails.mobile,
        "email" -> testContactDetails.email,
        //"webAddress" -> Do we need this?
        "commsPreference" -> "ZEL" //electronic
      )
    ),
    "subscription" -> Json.obj(
      "reasonForSubscription" -> Json.obj(
        "registrationReason" -> "0016",
        "relevantDate" -> testDate,
        "voluntaryOrEarlierDate" -> testDate,
        //For mandatory users - voluntary is optionally provided by the user
        //For voluntary users - relevant date = voluntaryOrEarlierDate
        "exemptionOrException" -> "0"
      ),
      "corporateBodyRegistered" -> Json.obj(
        "companyRegistrationNumber" -> testCrn,
        "dateOfIncorporation" -> testDateOfIncorp,
        "countryOfIncorporation" -> "GB"
      ),
      "businessActivities" -> Json.obj(
        "description" -> testSicAndCompliance.businessDescription,
        "SICCodes" -> Json.obj(
          "primaryMainCode" -> testSicAndCompliance.mainBusinessActivity.id,
        )
      ),
      "yourTurnover" -> Json.obj(
        "turnoverNext12Months" -> testEligibilitySubmissionData.estimates.turnoverEstimate,
        "zeroRatedSupplies" -> 12.99,
        "VATRepaymentExpected" -> true
      ),
      "schemes" -> Json.obj(
        "FRSCategory" -> frsDetails.categoryOfBusiness.get,
        "FRSPercentage" -> frsDetails.percent,
        "startDate" -> frsDetails.startDate.get,
        "limitedCostTrader" -> frsDetails.limitedCostTrader.get
      )
    ),
    "periods" -> Json.obj(
      "customerPreferredPeriodicity" -> "MA",
    ),
    "bankDetails" -> Json.obj(
      "UK" -> Json.obj(
        "accountName" -> testBankDetails.name,
        "accountNumber" -> testBankDetails.number,
        "sortCode" -> testBankDetails.sortCode
        // Missing bank account reason is being developed
      )
    ),
    "compliance" -> Json.obj(
      "supplyWorkers" -> testSicAndCompliance.labourCompliance.get.supplyWorkers,
      "numOfWorkersSupplied" -> testSicAndCompliance.labourCompliance.get.numOfWorkersSupplied.get,
      "intermediaryArrangement" -> testSicAndCompliance.labourCompliance.get.intermediaryArrangement.get
    ),
    "declaration" -> Json.obj(
      "applicantDetails" -> Json.obj(
        "roleInBusiness" -> "03",
        "name" -> Json.obj(
          "firstName" -> testName.first,
          "lastName" -> testName.last,
        ),
        "prevName" -> Json.obj(
          "firstName" -> testFormerName.name.get.first,
          "lastName" -> testFormerName.name.get.last,
          "nameChangeDate" -> testDate
        ),
        "currAddress" -> Json.obj(
          "line1" -> testAddress.line1,
          "line2" -> testAddress.line2,
          "postCode" -> testAddress.postcode,
          "countryCode" -> "GB",
          "addressValidated" -> true
        ),
        "commDetails" -> Json.obj(
          "email" -> testDigitalContactOptional.email.get,
        ),
        "dateOfBirth" -> testDate,
        "identifiers" -> Json.arr(
          Json.obj(
            "date" -> testDate,
            "idType" -> "NINO",
            "idValue" -> testNino,
            "IDsVerificationStatus" -> "1"
          )
        )
      ),
      "declarationSigning" -> Json.obj(
        "confirmInformationDeclaration" -> true,
        "declarationCapacity" -> "03" //currently defaulted company director
      )
    )
  )

  val testRegisteredBusinessPartnerSubmissionJson: JsObject = Json.obj(
    "messageType" -> "SubscriptionCreate",
    "admin" -> Json.obj(
      "additionalInformation" -> Json.obj(
        "customerStatus" -> "2"
      ),
      "attachments" -> Json.obj(
        "EORIrequested" -> true
      )
    ),
    "customerIdentification" -> Json.obj(
      "tradersPartyType" -> "50",
      "primeBPSafeID" -> testBpSafeId,
      "tradingName" -> testTradingDetails.tradingName.get,
      "shortOrgName" -> testCompanyName
    ),
    "contact" -> Json.obj(
      "address" -> Json.obj(
        "line1" -> testAddress.line1,
        "line2" -> testAddress.line2,
        //line5 not supplied by ALF
        "postCode" -> testAddress.postcode,
        "countryCode" -> "GB",
        "addressValidated" -> true //false if manually entered by user
      ),
      "commDetails" -> Json.obj(
        "telephone" -> testContactDetails.tel,
        "mobileNumber" -> testContactDetails.mobile,
        "email" -> testContactDetails.email,
        //"webAddress" -> Do we need this?
        "commsPreference" -> "ZEL" //electronic
      )
    ),
    "subscription" -> Json.obj(
      "reasonForSubscription" -> Json.obj(
        "registrationReason" -> "0016",
        "relevantDate" -> testDate,
        "voluntaryOrEarlierDate" -> testDate,
        //For mandatory users - voluntary is optionally provided by the user
        //For voluntary users - relevant date = voluntaryOrEarlierDate
        "exemptionOrException" -> "0"
      ),
      "corporateBodyRegistered" -> Json.obj(
        "companyRegistrationNumber" -> testCrn,
        "dateOfIncorporation" -> testDateOfIncorp,
        "countryOfIncorporation" -> "GB"
      ),
      "businessActivities" -> Json.obj(
        "description" -> testSicAndCompliance.businessDescription,
        "SICCodes" -> Json.obj(
          "primaryMainCode" -> testSicAndCompliance.mainBusinessActivity.id,
        )
      ),
      "yourTurnover" -> Json.obj(
        "turnoverNext12Months" -> testEligibilitySubmissionData.estimates.turnoverEstimate,
        "zeroRatedSupplies" -> 12.99,
        "VATRepaymentExpected" -> true
      )
    ),
    "periods" -> Json.obj(
      "customerPreferredPeriodicity" -> "MA",
    ),
    "bankDetails" -> Json.obj(
      "UK" -> Json.obj(
        "reasonBankAccNotProvided" -> "1"
      )
    ),
    "compliance" -> Json.obj(
      "supplyWorkers" -> testSicAndCompliance.labourCompliance.get.supplyWorkers,
      "numOfWorkersSupplied" -> testSicAndCompliance.labourCompliance.get.numOfWorkersSupplied.get,
      "intermediaryArrangement" -> testSicAndCompliance.labourCompliance.get.intermediaryArrangement.get
    ),
    "declaration" -> Json.obj(
      "applicantDetails" -> Json.obj(
        "roleInBusiness" -> "03",
        "name" -> Json.obj(
          "firstName" -> testName.first,
          "lastName" -> testName.last,
        ),
        "currAddress" -> Json.obj(
          "line1" -> testAddress.line1,
          "line2" -> testAddress.line2,
          "postCode" -> testAddress.postcode,
          "countryCode" -> "GB",
          "addressValidated" -> true
        ),
        "commDetails" -> Json.obj(
          "email" -> testDigitalContactOptional.email.get,
        ),
        "dateOfBirth" -> testDate,
        "identifiers" -> Json.arr(
          Json.obj(
            "date" -> testDate,
            "idType" -> "NINO",
            "idValue" -> testNino,
            "IDsVerificationStatus" -> "1"
          )
        )
      ),
      "declarationSigning" -> Json.obj(
        "confirmInformationDeclaration" -> true,
        "declarationCapacity" -> "03" //currently defaulted company director
      )
    )
  )
}

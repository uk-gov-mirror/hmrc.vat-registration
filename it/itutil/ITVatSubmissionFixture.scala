
package itutil

import play.api.libs.json.Json

trait ITVatSubmissionFixture {

  val vatSubmissionJson = Json.parse(
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
      |        "countryCode": "UK"
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
      |      "FRSPercentage": 15
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
      |    "numOfWorkers": 1000,
      |    "tempWorkers": true,
      |    "provisionOfLabour": true
      |  },
      |  "businessActivities": {
      |    "SICCodes": {
      |      "mainCode2": "00998",
      |      "primaryMainCode": "12345",
      |      "mainCode3": "00889"
      |    },
      |    "description": "this is my business description"
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

}

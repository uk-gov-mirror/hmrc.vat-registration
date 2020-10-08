
package itutil

import play.api.libs.json.Json

trait ITVatSubmissionFixture {

  val vatSubmissionJson = Json.parse(
    """
      |{
      |  "messageType": "SubmissionCreate",
      |  "admin": {
      |    "additionalInformation": {
      |      "customerStatus": "2"
      |    },
      |    "attachments": {
      |      "EORIRequested": true
      |    }
      |  },
      |  "customerIdentification": {
      |    "tradingName": "trading-name",
      |    "tradersPartyType": "50",
      |    "shortOrgName": "testCompanyName",
      |    "name": {
      |      "firstName": "Forename",
      |      "lastName": "Surname"
      |    },
      |    "customerID": [
      |      {
      |        "idValue": "testCtUtr",
      |        "idType": " UTR",
      |        "IDsVerificationStatus": "Verified"
      |      },
      |      {
      |        "idValue": "testCrn",
      |        "idType": "CRN",
      |        "IDsVerificationStatus": "Verified",
      |        "date": "2020-01-02"
      |      }
      |    ],
      |    "dateOfBirth": "2018-01-01",
      |    "primeBPSafeId": "12345678901234567890"
      |  },
      |  "declaration": {
      |    "declarationSigning": {
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
      |          "IDsVerificationStatus": "Verified",
      |          "date": "2018-01-01"
      |        },
      |        {
      |          "idValue": "testCrn",
      |          "idType": "CRN",
      |          "IDsVerificationStatus": "Verified",
      |          "date": "2020-01-02"
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
  "subscription": {
      |    "corporateBodyRegistered": {
      |        "companyRegistrationNumber": "testCrn",
      |        "dateOfIncorporation": "2020-01-02"
      |    },
      |    "reasonForSubscription": {
      |        "exemptionOrException": "0",
      |        "registrationReason": "0016",
      |        "relevantDate": "2020-10-07"
      |    },
      |    "schemes": {
      |        "FRSCategory": "testCategory",
      |        "FRSPercentage": 15,
      |        "startDate": "2018-01-01"
      |    },
      |    "yourTurnover": {
      |        "turnoverNext12Months": 123456
      |    }
      |  },
      |  "bankDetails": {
      |    "UK": {
      |      "accountName": "Test Bank Account",
      |      "accountNumber": "01023456",
      |      "sortCode": "010203"
      |    }
      |  },
      |  "businessActivities": {
      |    "description": "this is my business description",
      |    "SICCodes": {
      |      "mainCode2": "00998",
      |      "primaryMainCode": "12345",
      |      "mainCode3": "00889"
      |    }
      |  },
      |  "contact": {
      |    "commDetails": {
      |      "webAddress": "www.foo.com",
      |      "mobile": "54321",
      |      "tel": "12345",
      |      "email": "email@email.com"
      |    },
      |    "address": {
      |      "line1": "line1",
      |      "line2": "line2",
      |      "countryCode": "foo"
      |    }
      |  }
      |}""".stripMargin)

}

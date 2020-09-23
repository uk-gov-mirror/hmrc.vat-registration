package itutil

import play.api.libs.json.Json

trait ITVatSubmissionFixture {

  val vatSubmissionJson = Json.parse(
    """
      |{
      |  "messageType": "SubmissionCreate",
      |  "admin": {
      |    "additionalInformation": {
      |      "customerStatus": "3"
      |    },
      |    "attachments": {
      |      "EORIRequested": true
      |    }
      |  },
      |  "customerIdentification": {
      |    "tradingName": "trading-name",
      |    "tradersPartyType": "50",
      |    "customerID": {
      |      "idValue": "AB123456A",
      |      "idType": "NINO",
      |      "IDsVerificationStatus": "Verified"
      |    },
      |    "name": {
      |      "first": "Forename",
      |      "last": "Surname"
      |    },
      |    "dateOfBirth": "2018-01-01",
      |    "primeBPSafeId": "12345678901234567890"
      |  },
      |  "declaration": {
      |    "declarationSigning": {
      |      "confirmInformationDeclaration": true
      |    },
      |    "applicantDetails": {
      |      "currAddress": {
      |        "line1": "12 Lukewarm",
      |        "line2": "Oriental lane"
      |      },
      |      "name": {
      |        "first": "Forename",
      |        "last": "Surname"
      |      },
      |      "commDetails": {
      |        "email": "skylake@vilikariet.com"
      |      },
      |      "dateOfBirth": "2018-01-01",
      |      "roleInBusiness": "secretary"
      |    }
      |  },
      |  "subscription": {
      |    "corporateBodyRegistered": {
      |      "companyRegistrationNumber": "testCrn"
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
      |  "businessActivities": {
      |    "description": "this is my business description",
      |    "SICCodes": {
      |      "primaryMainCode": "12345",
      |      "mainCode2": "00998",
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

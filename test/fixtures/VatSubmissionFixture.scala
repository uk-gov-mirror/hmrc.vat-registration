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

package fixtures

import play.api.libs.json.Json

trait VatSubmissionFixture {

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
      |  "subscription": {
      |    "corporateBodyRegistered": {
      |      "dateOfIncorporation": "2020-01-02",
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

  val mongoJson = Json.parse(
    """
      |{
      |  "messageType": "SubmissionCreate",
      |  "customerStatus": "3",
      |  "tradersPartyType": "50",
      |  "primeBPSafeId": "12345678901234567890",
      |  "confirmInformationDeclaration": true,
      |  "companyRegistrationNumber": "testCrn",
      |  "applicantDetails": {
      |    "nino": "AB123456A",
      |    "role": "secretary",
      |    "name": {
      |      "first": "Forename",
      |      "last": "Surname"
      |    },
      |    "dateOfBirth": "2018-01-01",
      |    "companyName": "testCompanyName",
      |    "companyNumber": "testCrn",
      |    "dateOfIncorporation": "2020-01-02",
      |    "ctutr": "testCtUtr",
      |    "currentAddress": {
      |      "line1": "line1",
      |      "line2": "line2",
      |      "postcode": "XX XX",
      |      "country": "UK"
      |    },
      |    "contact": {
      |      "email": "skylake@vilikariet.com"
      |    },
      |    "changeOfName": {
      |      "name": {
      |        "first": "Forename",
      |        "last": "Surname"
      |      },
      |      "change": "2018-01-01"
      |    }
      |  },
      |  "bankDetails": {
      |    "name": "Test Bank Account",
      |    "sortCode": "010203",
      |    "number": "01023456"
      |  },
      |  "sicAndCompliance": {
      |    "businessDescription": "this is my business description",
      |    "labourCompliance": {
      |      "numberOfWorkers": 1000,
      |      "temporaryContracts": true,
      |      "skilledWorkers": true
      |    },
      |    "mainBusinessActivity": {
      |      "code": "12345",
      |      "desc": "the flu",
      |      "indexes": "sic details"
      |    },
      |    "otherBusinessActivities": [
      |      {
      |        "code": "00998",
      |        "desc": "otherBusiness desc 1",
      |        "indexes": "fooBar 1"
      |      },
      |      {
      |        "code": "00889",
      |        "desc": "otherBusiness desc 2",
      |        "indexes": "fooBar 2"
      |      }
      |    ]
      |  },
      |  "businessContact": {
      |    "digitalContact": {
      |      "email": "email@email.com",
      |      "tel": "12345",
      |      "mobile": "54321"
      |    },
      |    "website": "www.foo.com",
      |    "ppob": {
      |      "line1": "line1",
      |      "line2": "line2",
      |      "country": "foo"
      |    }
      |  },
      |  "tradingDetails": {
      |    "tradingName": "trading-name",
      |    "eoriRequested": true
      |  },
      |  "flatRateScheme": {
      |    "businessGoods": {
      |      "estimatedTotalSales": 1234567891011,
      |      "overTurnover": true
      |    },
      |    "startDate": "2018-01-01",
      |    "categoryOfBusiness": "testCategory",
      |    "percent": 15
      |  }
      |}""".stripMargin)

}

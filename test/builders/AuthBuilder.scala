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

package builders

import connectors.{AuthConnector, Authority, UserIds}
import org.mockito.Mockito._
import play.api.mvc._
import play.api.test.FakeRequest

import scala.concurrent.Future
import uk.gov.hmrc.http.HeaderCarrier

object AuthBuilder extends AuthBuilder {}

trait AuthBuilder {

  implicit val testHeaderCarrier: HeaderCarrier = HeaderCarrier()

  def mockAuthorisedUser(userId: String, mockAuthConnector: AuthConnector) {
    when(mockAuthConnector.getCurrentAuthority()).thenReturn(Future.successful(Some(Authority(userId, userId, userId, UserIds(userId, userId)))))
  }

  def withAuthorisedUser(action: Action[AnyContent], mockAuthConnector: AuthConnector)(test: Future[Result] => Any) {
    val userId = "testUserId"
    mockAuthorisedUser(userId, mockAuthConnector)
    val result = action(FakeRequest())
    test(result)
  }

  def submitWithUnauthorisedUser(action: Action[AnyContent], mockAuthConnector: AuthConnector, request: FakeRequest[AnyContentAsFormUrlEncoded])
                                (test: Future[Result] => Any) {
    when(mockAuthConnector.getCurrentAuthority()).thenReturn(Future.successful(None))
    val result = action.apply(SessionBuilder.updateRequestFormWithSession(request, ""))
    test(result)
  }

  def submitWithAuthorisedUser(action: Action[AnyContent], mockAuthConnector: AuthConnector, request: FakeRequest[AnyContentAsFormUrlEncoded])
                              (test: Future[Result] => Any) {
    val userId = "testUserId"
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    val result = action.apply(SessionBuilder.updateRequestFormWithSession(request, userId))
    test(result)
  }

}

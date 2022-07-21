/*
 * Copyright 2022 HM Revenue & Customs
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

package controllers.admin

import models.admin.FeatureFlagName.OnlinePaymentIntegration
import models.admin.{FeatureFlag, FeatureFlagName}
import org.mockito.ArgumentMatchers.{any, eq => meq}
import org.mockito.Mockito.{reset, when}
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.cache.AsyncCacheApi
import play.api.http.Status.OK
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.JsBoolean
import play.api.mvc.{AnyContentAsEmpty, ControllerComponents}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.AdminRepository
import services.admin.FeatureFlagService
import uk.gov.hmrc.http.UpstreamErrorResponse
import uk.gov.hmrc.internalauth.client.Predicate.Permission
import uk.gov.hmrc.internalauth.client.test.{BackendAuthComponentsStub, StubBehaviour}
import uk.gov.hmrc.internalauth.client._
import util.BaseSpec

import scala.concurrent.Future

class FeatureFlagControllerSpec extends BaseSpec with GuiceOneAppPerSuite {

  def fakeRequest(method: String, uri: String): FakeRequest[AnyContentAsEmpty.type] =
    FakeRequest(method, uri)

  val cc: ControllerComponents = stubControllerComponents()

  val mockStubBehaviour: StubBehaviour = mock[StubBehaviour]

  val mockAdminRepository: AdminRepository = mock[AdminRepository]

  val mockFeatureFlagService: FeatureFlagService = mock[FeatureFlagService]

  val flags: Seq[FeatureFlag] =
    Seq(FeatureFlag(OnlinePaymentIntegration, enabled = true))

  val permission: Permission =
    Permission(
      resource = Resource(
        resourceType = ResourceType("ddcn-live-admin-frontend"),
        resourceLocation = ResourceLocation("*")
      ),
      action = IAAction("ADMIN")
    )

  def application: GuiceApplicationBuilder =
    new GuiceApplicationBuilder()
      .overrides(
        bind[AsyncCacheApi].toInstance(mockCacheApi),
        bind[AdminRepository].to(mockAdminRepository),
        bind[FeatureFlagService].to(mockFeatureFlagService),
        bind(classOf[BackendAuthComponents]).toInstance(BackendAuthComponentsStub(mockStubBehaviour)(cc, ec))
      )

  override def beforeEach(): Unit =
    reset(
      mockAdminRepository,
      mockFeatureFlagService
    )

  "Feature Flag Controller" must {

    "return OK for a GET request" in {
      val app = application.build()

      when(mockStubBehaviour.stubAuth[Unit](meq(Some(permission)), any()))
        .thenReturn(Future.successful(()))

      when(mockFeatureFlagService.getAll)
        .thenReturn(Future.successful(flags))

      running(app) {
        val request =
          fakeRequest(GET, routes.FeatureFlagController.get.url)
            .withHeaders("Authorization" -> "some-token")

        val result = route(app, request).head

        status(result) shouldBe OK
      }
    }

    "return UNAUTHORIZED for a GET request" in {
      val app = application.build()

      when(mockStubBehaviour.stubAuth[Unit](meq(Some(permission)), any()))
        .thenReturn(Future.failed(UpstreamErrorResponse("Unauthorised", UNAUTHORIZED)))

      running(app) {
        val request = fakeRequest(GET, routes.FeatureFlagController.get.url)

        val result = route(app, request).head

        whenReady(result.failed) { case e: UpstreamErrorResponse =>
          e.statusCode shouldBe UNAUTHORIZED
        }
      }
    }

    "return NoContent for a successful PUT request" in {
      val app = application.build()

      when(mockStubBehaviour.stubAuth[Unit](meq(Some(permission)), any()))
        .thenReturn(Future.successful(()))

      when(mockFeatureFlagService.set(any(), any()))
        .thenReturn(Future.successful(true))

      running(app) {
        val request = fakeRequest(
          PUT,
          routes.FeatureFlagController.put(FeatureFlagName.OnlinePaymentIntegration).url
        )
          .withBody(JsBoolean(true))
          .withHeaders("Authorization" -> "some-token")

        val result = route(app, request).head

        status(result) shouldBe NO_CONTENT
      }
    }

    "return BadRequest for a PUT request with an invalid payload" in {
      val app = application.build()

      when(mockStubBehaviour.stubAuth[Unit](meq(Some(permission)), any()))
        .thenReturn(Future.successful(()))

      when(mockFeatureFlagService.set(any(), any()))
        .thenReturn(Future.successful(false))

      running(app) {
        val request = fakeRequest(
          PUT,
          routes.FeatureFlagController.put(FeatureFlagName.OnlinePaymentIntegration).url
        )
          .withBody("This is not a valid request body")
          .withHeaders("Authorization" -> "some-token")

        val result = route(app, request).head

        status(result) shouldBe BAD_REQUEST
      }
    }

  }

}

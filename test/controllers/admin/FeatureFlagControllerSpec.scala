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

import akka.Done
import models.admin.FeatureFlagName.OnlinePaymentIntegration
import models.admin.{FeatureFlag, FeatureFlagName}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.cache.AsyncCacheApi
import play.api.http.Status.OK
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.JsBoolean
import play.api.mvc.AnyContentAsEmpty
import play.api.test.CSRFTokenHelper.CSRFFRequestHeader
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.AdminRepository
import services.admin.FeatureFlagService
import util.BaseSpec

import scala.concurrent.Future
import scala.concurrent.duration.Duration
import scala.reflect.ClassTag

class FeatureFlagControllerSpec extends BaseSpec with GuiceOneAppPerSuite {

  def fakeCSRFRequest(method: String, uri: String): FakeRequest[AnyContentAsEmpty.type] =
    FakeRequest(method, uri).withCSRFToken.asInstanceOf[FakeRequest[AnyContentAsEmpty.type]]

  val mockAdminRepository: AdminRepository = mock[AdminRepository]

  val mockFeatureFlagService: FeatureFlagService = mock[FeatureFlagService]

  val flags: Seq[FeatureFlag] =
    Seq(FeatureFlag(OnlinePaymentIntegration, enabled = true))

  "Feature Flag Controller" must {

    "return OK with the service configuration for a GET request" in {
      when(mockFeatureFlagService.getAll).thenReturn(Future.successful(flags))

      val application =
        new GuiceApplicationBuilder()
          .overrides(
            bind[AsyncCacheApi].toInstance(mockCacheApi),
            bind[AdminRepository].to(mockAdminRepository),
            bind[FeatureFlagService].to(mockFeatureFlagService)
          )
          .build()

      running(application) {
        val request = fakeCSRFRequest(GET, routes.FeatureFlagController.get.url)

        val result = route(application, request).head

        status(result) shouldBe OK
      }
    }

    "return NoContent for a successful PUT request" in {
      when(mockFeatureFlagService.set(any(), any())).thenReturn(Future.successful(true))

      val application =
        new GuiceApplicationBuilder()
          .overrides(
            bind[AsyncCacheApi].toInstance(mockCacheApi),
            bind[AdminRepository].to(mockAdminRepository),
            bind[FeatureFlagService].to(mockFeatureFlagService)
          )
          .build()

      running(application) {
        val request = fakeCSRFRequest(
          PUT,
          routes.FeatureFlagController.put(FeatureFlagName.OnlinePaymentIntegration).url
        ).withBody(JsBoolean(true))

        val result = route(application, request).head

        status(result) shouldBe NO_CONTENT
      }
    }

    "return BadRequest for a PUT request with an invalid payload" in {
      when(mockFeatureFlagService.set(any(), any())).thenReturn(Future.successful(false))

      val application =
        new GuiceApplicationBuilder()
          .overrides(
            bind[AsyncCacheApi].toInstance(mockCacheApi),
            bind[AdminRepository].to(mockAdminRepository),
            bind[FeatureFlagService].to(mockFeatureFlagService)
          )
          .build()

      running(application) {
        val request = fakeCSRFRequest(
          PUT,
          routes.FeatureFlagController.put(FeatureFlagName.OnlinePaymentIntegration).url
        ).withBody("This is not a valid request body")

        val result = route(application, request).head

        status(result) shouldBe BAD_REQUEST
      }
    }

  }

}

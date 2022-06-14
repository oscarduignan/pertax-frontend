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

package services.admin

import akka.Done
import models.admin.FeatureFlag.{Disabled, Enabled}
import models.admin.FeatureFlagName.OnlinePaymentIntegration
import models.admin.{FeatureFlag, FeatureFlagName, FeatureFlags}
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{times, verify, when}
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.{Arbitrary, Gen}
import org.scalatest.concurrent.ScalaFutures
import play.api.cache.AsyncCacheApi
import repositories.AdminRepository
import util.BaseSpec

import scala.concurrent.Future
import scala.concurrent.duration.Duration
import scala.reflect.ClassTag

class FeatureFlagServiceSpec extends BaseSpec with ScalaFutures {

  implicit private val arbitraryFeatureFlagName: Arbitrary[FeatureFlagName] = Arbitrary {
    Gen.oneOf(FeatureFlagName.flags)
  }

  // A cache that doesn't cache
  class FakeCache extends AsyncCacheApi {
    override def set(key: String, value: Any, expiration: Duration): Future[Done] = ???

    override def remove(key: String): Future[Done] = ???

    override def getOrElseUpdate[A](key: String, expiration: Duration)(orElse: => Future[A])(implicit
      evidence$1: ClassTag[A]
    ): Future[A] = orElse

    override def get[T](key: String)(implicit evidence$2: ClassTag[T]): Future[Option[T]] = ???

    override def removeAll(): Future[Done] = ???
  }

  "When set works in the repo returns true" in {
    val adminRepository = mock[AdminRepository]
    when(adminRepository.getFeatureFlags).thenReturn(Future.successful(FeatureFlags(Seq.empty)))
    when(adminRepository.setFeatureFlags(any())).thenReturn(Future.successful(true))

    val OUT = new FeatureFlagService(adminRepository, new FakeCache())
    val flagName = arbitrary[FeatureFlagName].sample.getOrElse(FeatureFlagName.OnlinePaymentIntegration)

    whenReady(OUT.set(flagName, enabled = true)) { result =>
      result mustBe true
      val captor = ArgumentCaptor.forClass(classOf[Seq[FeatureFlag]])
      verify(adminRepository, times(1)).setFeatureFlags(captor.capture())
      captor.getValue must contain(Enabled(flagName))
    }
  }

  "When set fails in the repo returns false" in {
    val adminRepository = mock[AdminRepository]

    val flagName = arbitrary[FeatureFlagName].sample.getOrElse(FeatureFlagName.OnlinePaymentIntegration)
    val OUT = new FeatureFlagService(adminRepository, new FakeCache())

    when(adminRepository.getFeatureFlags).thenReturn(Future.successful(FeatureFlags(Seq.empty)))
    when(adminRepository.setFeatureFlags(any())).thenReturn(Future.successful(false))

    whenReady(OUT.set(flagName, enabled = true))(_ mustBe false)
  }

  "When getAll is called returns all of the flags from the repo" in {
    val adminRepository = mock[AdminRepository]
    val OUT = new FeatureFlagService(adminRepository, new FakeCache())

    when(adminRepository.getFeatureFlags).thenReturn(Future.successful(FeatureFlags(Seq.empty)))

    OUT.getAll.futureValue mustBe Seq(Enabled(OnlinePaymentIntegration))
  }

  "When a flag doesn't exist in the repo the default is returned" in {
    val adminRepository = mock[AdminRepository]
    when(adminRepository.getFeatureFlags).thenReturn(Future.successful(FeatureFlags(Seq.empty)))
    val OUT = new FeatureFlagService(adminRepository, new FakeCache())
    OUT.get(OnlinePaymentIntegration).futureValue mustBe Enabled(OnlinePaymentIntegration)
  }

  "When a flag exists in the repo that overrides the default" in {
    val adminRepository = mock[AdminRepository]
    when(adminRepository.getFeatureFlags)
      .thenReturn(Future.successful(FeatureFlags(Seq(Disabled(OnlinePaymentIntegration)))))
    val OUT = new FeatureFlagService(adminRepository, new FakeCache())
    OUT.get(OnlinePaymentIntegration).futureValue mustBe Disabled(OnlinePaymentIntegration)
  }
}

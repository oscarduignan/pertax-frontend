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

import models.admin.FeatureFlag.{Disabled, Enabled}
import models.admin.FeatureFlagName.OnlinePaymentIntegration
import models.admin.{FeatureFlag, FeatureFlagName}
import play.api.cache.AsyncCacheApi
import repositories.AdminRepository

import javax.inject.{Inject, Singleton}
import scala.concurrent.duration.{Duration, FiniteDuration, SECONDS => Seconds}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class FeatureFlagService @Inject() (
  adminRepository: AdminRepository,
  cache: AsyncCacheApi
)(implicit
  ec: ExecutionContext
) {
  val cacheValidFor: FiniteDuration =
    Duration(2, Seconds)

  private val defaults: Seq[FeatureFlag] =
    Seq(Enabled(OnlinePaymentIntegration))

  private def addDefaults(fromDb: Seq[FeatureFlag]): Seq[FeatureFlag] =
    fromDb ++ defaults
      .filterNot(default =>
        fromDb
          .exists(fdb => fdb.name == default.name)
      )

  def getAll: Future[Seq[FeatureFlag]] =
    cache
      .getOrElseUpdate[Seq[FeatureFlag]]("feature-flags", cacheValidFor) {
        adminRepository.getFeatureFlags
          .map {
            case Some(ff) =>
              addDefaults(ff)
            case None =>
              addDefaults(Seq.empty[FeatureFlag])
          }
      }

  def set(flagName: FeatureFlagName, enabled: Boolean): Future[Boolean] =
    getAll
      .flatMap { currentFlags =>
        val updatedFlags =
          currentFlags
            .filterNot(_.name == flagName) :+ FeatureFlag(flagName, enabled)

        adminRepository
          .setFeatureFlags(updatedFlags)
      }

  def get(name: FeatureFlagName): Future[FeatureFlag] =
    getAll
      .map { flags =>
        flags
          .find(_.name == name)
          .getOrElse(Disabled(name))
      }
}

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

package repositories

import models.admin._
import org.mongodb.scala.model.Filters._
import org.mongodb.scala.model.Indexes._
import org.mongodb.scala.model.Projections._
import org.mongodb.scala.model.Updates.set
import org.mongodb.scala.model.{IndexModel, IndexOptions, UpdateOptions}
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.{Codecs, PlayMongoRepository}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AdminRepository @Inject() (
  mongoComponent: MongoComponent
)(implicit
  ec: ExecutionContext
) extends PlayMongoRepository[FeatureFlags](
      collectionName = "admin-data",
      mongoComponent = mongoComponent,
      domainFormat = FeatureFlagMongoFormats.formats,
      indexes = Seq(
        IndexModel(
          keys = ascending("feature-flags"),
          indexOptions = IndexOptions()
            .name("flags")
            .unique(true)
        )
      ),
      extraCodecs = Seq(
        Codecs.playFormatCodec(FeatureFlag.formats),
        Codecs.playFormatCodec(FeatureFlagName.formats)
      )
    ) {

  def getFeatureFlags: Future[FeatureFlags] =
    collection
      .find()
      .projection(excludeId())
      .toSingle()
      .toFuture()

  def setFeatureFlags(featureFlags: Seq[FeatureFlag]): Future[Boolean] =
    collection
      .updateOne(
        filter = equal("_id", "feature-flags"),
        update = set("flags", Codecs.toBson(featureFlags)),
        options = UpdateOptions().upsert(true)
      )
      .map(_.wasAcknowledged())
      .toSingle()
      .toFuture()
}

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

package services

import cats.implicits._
import com.google.inject.Inject
import config.ConfigDecorator
import connectors.SeissConnector
import models.{SelfAssessmentUser, SelfAssessmentUserType}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

class SeissService @Inject() (seissConnector: SeissConnector, appConfig: ConfigDecorator)(implicit
  ec: ExecutionContext
) {

  def hasClaims(saUserType: SelfAssessmentUserType)(implicit hc: HeaderCarrier): Future[Boolean] =
    if (appConfig.isSeissTileEnabled) {
      saUserType match {
        case user: SelfAssessmentUser =>
          seissConnector.getClaims(user.saUtr.utr).bimap(_ => false, claims => claims.nonEmpty).merge
        case _ => Future.successful(false)
      }
    } else {
      Future.successful(false)
    }
}

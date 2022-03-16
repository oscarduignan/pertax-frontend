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

import cats.data.EitherT
import cats.implicits._
import com.google.inject.Inject
import connectors.AgentClientAuthorisationConnector
import models.AgentClientStatus
import play.api.mvc.Request
import repositories.SessionCacheRepository
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}
import uk.gov.hmrc.mongo.cache.DataKey
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import scala.concurrent.{ExecutionContext, Future}

class AgentClientAuthorisationService @Inject() (
  servicesConfig: ServicesConfig,
  agentClientAuthorisationConnector: AgentClientAuthorisationConnector,
  cache: SessionCacheRepository
) {

  private val agentClientStatusDataKey = DataKey[AgentClientStatus]("agentClientStatus")
  lazy val agentClientAuthorisationCacheEnabled =
    servicesConfig.getConfBool("feature.agent-client-authorisation.cached", true)
  lazy val agentClientAuthorisationEnabled =
    servicesConfig.getConfBool("feature.agent-client-authorisation.enabled", true)

  def getAgentClientStatus(implicit hc: HeaderCarrier, ec: ExecutionContext, request: Request[_]): Future[Boolean] =
    if (agentClientAuthorisationEnabled) {
      (if (agentClientAuthorisationCacheEnabled) {
         EitherT(
           cache
             .getFromSession[AgentClientStatus](agentClientStatusDataKey)
             .map {
               case Some(agentClientStatus: AgentClientStatus) =>
                 EitherT.rightT[Future, UpstreamErrorResponse](agentClientStatus)
               case _ =>
                 agentClientAuthorisationConnector.getAgentClientStatus.map(response =>
                   cache.putSession(agentClientStatusDataKey, response)
                 )
             }
             .map(_.value)
             .flatten
         )
       } else agentClientAuthorisationConnector.getAgentClientStatus)
        .bimap(
          _ => false,
          {
            case AgentClientStatus(false, false, false) => false
            case _                                      => true
          }
        )
        .merge
    } else Future.successful(false)
}

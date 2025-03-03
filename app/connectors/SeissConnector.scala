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

package connectors

import cats.data.EitherT
import cats.implicits._
import com.codahale.metrics.Timer
import com.google.inject.Inject
import config.ConfigDecorator
import metrics.{Metrics, MetricsEnumeration}
import models.{SeissModel, SeissRequest}
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse, UpstreamErrorResponse}

import javax.inject.Singleton
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SeissConnector @Inject() (
  http: HttpClient,
  metrics: Metrics,
  httpClientResponse: HttpClientResponse,
  implicit val ec: ExecutionContext,
  configDecorator: ConfigDecorator
) {

  def getClaims(utr: String)(implicit hc: HeaderCarrier): EitherT[Future, UpstreamErrorResponse, List[SeissModel]] = {
    val seissRequest = SeissRequest(utr)

    val timerContext: Timer.Context =
      metrics.startTimer(MetricsEnumeration.GET_SEISS_CLAIMS)

    val response = http
      .POST[SeissRequest, Either[UpstreamErrorResponse, HttpResponse]](
        s"${configDecorator.seissUrl}/self-employed-income-support/get-claims",
        seissRequest
      )
      .map { response =>
        timerContext.stop()
        response
      }
    httpClientResponse.read(response, MetricsEnumeration.GET_SEISS_CLAIMS).map { response =>
      response.json.as[List[SeissModel]]
    }
  }
}

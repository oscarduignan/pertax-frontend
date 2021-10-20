/*
 * Copyright 2021 HM Revenue & Customs
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

package util

import com.google.inject.Inject
import metrics.HasMetrics
import play.api.Logging
import play.api.mvc.RequestHeader
import uk.gov.hmrc.http.{HttpException, HttpGet}
import uk.gov.hmrc.play.partials.HtmlPartial._
import uk.gov.hmrc.play.partials.{HeaderCarrierForPartialsConverter, HtmlPartial}

import scala.concurrent.{ExecutionContext, Future}

abstract class EnhancedPartialRetriever @Inject() (
  headerCarrierForPartialsConverter: HeaderCarrierForPartialsConverter
)(implicit executionContext: ExecutionContext)
    extends HasMetrics with Logging {

  def http: HttpGet

  def loadPartial(url: String)(implicit request: RequestHeader): Future[HtmlPartial] =
    withMetricsTimer("load-partial") { timer =>
      implicit val hc = headerCarrierForPartialsConverter.fromRequestWithEncryptedCookie(request)

      http.GET[HtmlPartial](url) map {
        case partial: HtmlPartial.Success =>
          timer.completeTimerAndIncrementSuccessCounter()
          partial
        case partial: HtmlPartial.Failure =>
          logger.error(s"Failed to load partial from $url, partial info: $partial")
          timer.completeTimerAndIncrementFailedCounter()
          partial
      } recover { case e =>
        timer.completeTimerAndIncrementFailedCounter()
        logger.error(s"Failed to load partial from $url", e)
        e match {
          case ex: HttpException =>
            HtmlPartial.Failure(Some(ex.responseCode))
          case _ =>
            HtmlPartial.Failure(None)
        }
      }

    }
}

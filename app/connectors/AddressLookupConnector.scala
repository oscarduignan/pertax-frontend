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

package connectors

import com.kenshoo.play.metrics.Metrics
import config.ConfigDecorator
import javax.inject.Inject
import metrics.HasMetrics
import models.addresslookup.AddressLookupConfig
import play.api.http.Status.{ACCEPTED, INTERNAL_SERVER_ERROR, OK}
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.http.{HeaderCarrier, HttpReads, HttpResponse}
import uk.gov.hmrc.play.bootstrap.http.HttpClient

import scala.concurrent.{ExecutionContext, Future}

class AddressLookupConnector @Inject()(http: HttpClient, configDecorator: ConfigDecorator, val metrics: Metrics)
    extends HasMetrics {

  def initJourney(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[HttpResponse] = {
    val url = s"${configDecorator.addressLookupFrontendService}/api/init"
    val json = Json.toJson(AddressLookupConfig.default(configDecorator))
    withMetricsTimer("init-address-lookup-journey") { timer =>
      http.POST[JsValue, HttpResponse](url, json) map { response =>
        response.status match {
          case ACCEPTED =>
            timer.completeTimerAndIncrementSuccessCounter()
            response
          case _ =>
            timer.completeTimerAndIncrementFailedCounter()
            response
        }
      } recover {
        case e: Exception =>
          timer.completeTimerAndIncrementFailedCounter()
          HttpResponse(INTERNAL_SERVER_ERROR, e.toString)
      }
    }
  }

  def getConfirmedAddress(id: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[HttpResponse] = {
    val url = s"${configDecorator.addressLookupFrontendService}/api/confirmed"
    withMetricsTimer("get-confirmed-address") { timer =>
      http.GET[HttpResponse](url, Seq(("id", id))) map { response =>
        response.status match {
          case OK =>
            timer.completeTimerAndIncrementSuccessCounter()
            response
          case _ =>
            timer.completeTimerAndIncrementFailedCounter()
            response
        }
      } recover {
        case e: Exception =>
          timer.completeTimerAndIncrementFailedCounter()
          HttpResponse(INTERNAL_SERVER_ERROR, e.toString)
      }
    }
  }

  implicit val httpReads: HttpReads[HttpResponse] = new HttpReads[HttpResponse] {
    override def read(method: String, url: String, response: HttpResponse) = response
  }
}

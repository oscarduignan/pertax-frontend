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

package services

import cats.data.OptionT
import com.google.inject.{Inject, Singleton}
import com.kenshoo.play.metrics.Metrics
import config.ConfigDecorator
import connectors.AddressLookupConnector
import metrics._
import models.addresslookup._
import play.api.Logger
import play.api.http.Status.{ACCEPTED, OK}
import services.http.SimpleHttp
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import util._

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AddressLookupService @Inject()(
  addressLookupConnector: AddressLookupConnector,
  configDecorator: ConfigDecorator,
  val simpleHttp: SimpleHttp,
  val metrics: Metrics,
  val tools: Tools,
  servicesConfig: ServicesConfig)
    extends HasMetrics {

  lazy val addressLookupUrl = servicesConfig.baseUrl("address-lookup")

  def lookup(postcode: String, filter: Option[String] = None)(
    implicit hc: HeaderCarrier): Future[AddressLookupResponse] =
    withMetricsTimer("address-lookup") { t =>
      val hn = tools.urlEncode(filter.getOrElse(""))
      val pc = postcode.replaceAll(" ", "")
      val newHc = hc.withExtraHeaders("X-Hmrc-Origin" -> configDecorator.origin)

      simpleHttp.get[AddressLookupResponse](s"$addressLookupUrl/v1/gb/addresses.json?postcode=$pc&filter=$hn")(
        onComplete = {
          case r if r.status >= 200 && r.status < 300 =>
            t.completeTimerAndIncrementSuccessCounter()
            AddressLookupSuccessResponse(RecordSet.fromJsonAddressLookupService(r.json))

          case r =>
            t.completeTimerAndIncrementFailedCounter()
            Logger.warn(s"Unexpected ${r.status} response getting address record from address lookup service")
            AddressLookupUnexpectedResponse(r)
        },
        onError = {
          case e =>
            t.completeTimerAndIncrementFailedCounter()
            Logger.warn("Error getting address record from address lookup service", e)
            AddressLookupErrorResponse(e)
        }
      )(newHc)
    }

  def initialiseAddressLookupJourney(implicit hc: HeaderCarrier, ec: ExecutionContext): OptionT[Future, String] =
    OptionT {
      addressLookupConnector.initJourney.map { response =>
        if (response.status == ACCEPTED) response.header("Location") else None
      }
    }

  def getAddressFromLookup(
    id: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): OptionT[Future, AddressLookupResponseV2] =
    OptionT {
      addressLookupConnector.getConfirmedAddress(id) map { response =>
        if (response.status == OK) response.json.asOpt[AddressLookupResponseV2] else None
      }
    }
}

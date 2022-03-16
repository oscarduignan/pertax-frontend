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

package metrics

import com.codahale.metrics.Timer
import com.codahale.metrics.Timer.Context
import com.google.inject.ImplementedBy
import metrics.MetricsEnumeration.MetricsEnumeration

import javax.inject.Inject

@ImplementedBy(classOf[MetricsImpl])
trait Metrics {

  def startTimer(api: MetricsEnumeration): Timer.Context

  def incrementSuccessCounter(api: MetricsEnumeration): Unit

  def incrementFailedCounter(api: MetricsEnumeration): Unit

}

class MetricsImpl @Inject() (metrics: com.kenshoo.play.metrics.Metrics) extends Metrics {

  val timers = Map(
    MetricsEnumeration.GET_AGENT_CLIENT_STATUS -> metrics.defaultRegistry
      .timer("get-agent-client-status-timer")
  )

  val successCounters = Map(
    MetricsEnumeration.GET_AGENT_CLIENT_STATUS -> metrics.defaultRegistry
      .counter("get-agent-client-status-success-counter")
  )

  val failedCounters = Map(
    MetricsEnumeration.GET_AGENT_CLIENT_STATUS -> metrics.defaultRegistry
      .counter("get-agent-client-status-failed-counter")
  )

  override def startTimer(api: MetricsEnumeration): Context = timers(api).time()

  override def incrementSuccessCounter(api: MetricsEnumeration): Unit =
    successCounters(api).inc()

  override def incrementFailedCounter(api: MetricsEnumeration): Unit =
    failedCounters(api).inc()
}

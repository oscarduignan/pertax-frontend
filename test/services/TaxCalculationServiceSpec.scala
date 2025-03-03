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

import com.codahale.metrics.Timer
import com.kenshoo.play.metrics.Metrics
import models.{OverpaidStatus, Reconciliation, TaxYearReconciliation}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.http.Status._
import play.api.libs.json.Json
import services.http.FakeSimpleHttp
import testUtils.{BaseSpec, Fixtures}
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.play.bootstrap.http.DefaultHttpClient
import testUtils.BaseSpec

import scala.concurrent.Future

class TaxCalculationServiceSpec extends BaseSpec {

  trait SpecSetup {
    def httpResponse: HttpResponse
    def simulateTaxCalculationServiceIsDown: Boolean

    val taxCalcDetails = Fixtures.buildTaxCalculation
    val jsonTaxCalcDetails = Json.toJson(taxCalcDetails)
    val anException = new RuntimeException("Any")
    val metricId = "get-taxcalc-summary"
    val mockHttp = mock[DefaultHttpClient]

    lazy val (service, metrics, timer) = {
      val fakeSimpleHttp = {
        if (simulateTaxCalculationServiceIsDown) new FakeSimpleHttp(Right(anException))
        else new FakeSimpleHttp(Left(httpResponse))
      }
      val serviceConfig = app.injector.instanceOf[ServicesConfig]

      val timer = mock[Timer.Context]
      val taxCalculationService: TaxCalculationService =
        new TaxCalculationService(fakeSimpleHttp, mock[Metrics], mockHttp, serviceConfig) {

          override val metricsOperator: MetricsOperator = mock[MetricsOperator]
          when(metricsOperator.startTimer(any())) thenReturn timer
        }

      (taxCalculationService, taxCalculationService.metricsOperator, timer)
    }
  }

  "Calling TaxCalculationService.getTaxYearReconciliations" must {

    "return a list of TaxYearReconciliations when given a valid nino" in new SpecSetup {
      override def httpResponse: HttpResponse = HttpResponse(OK, Some(jsonTaxCalcDetails))
      override def simulateTaxCalculationServiceIsDown: Boolean = false

      val expectedTaxYearList =
        Future(List(TaxYearReconciliation(2019, Reconciliation.overpaid(Some(100), OverpaidStatus.Refund))))

      val fakeNino = Fixtures.fakeNino

      when(mockHttp.GET[List[TaxYearReconciliation]](any(), any(), any())(any(), any(), any()))
        .thenReturn(expectedTaxYearList)

      val result = service.getTaxYearReconciliations(fakeNino)

      whenReady(result) {
        _ mustBe expectedTaxYearList.futureValue
      }

    }

    "return nil when unable to get a list of TaxYearReconciliation" in new SpecSetup {
      override def httpResponse: HttpResponse = HttpResponse(OK, Some(jsonTaxCalcDetails))
      override def simulateTaxCalculationServiceIsDown: Boolean = false

      val expectedTaxYearList = Nil

      val fakeNino = Fixtures.fakeNino

      when(mockHttp.GET[List[TaxYearReconciliation]](any(), any(), any())(any(), any(), any()))
        .thenReturn(Future.failed(new RuntimeException))

      val result = service.getTaxYearReconciliations(fakeNino)

      whenReady(result) {
        _ mustBe expectedTaxYearList
      }
    }
  }

}

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

import java.util.UUID

import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, badRequest, get, notFound, ok, post, serverError, urlEqualTo}
import com.github.tomakehurst.wiremock.http.Fault
import org.scalatestplus.mockito.MockitoSugar
import play.api.Application
import play.api.http.Status._
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import util.{BaseSpec, WireMockHelper}

import scala.concurrent.ExecutionContext

class AddressLookupConnectorSpec extends BaseSpec with WireMockHelper with MockitoSugar {

  override implicit lazy val app: Application = new GuiceApplicationBuilder()
    .configure(
      "microservice.services.address-lookup-frontend.port" -> server.port()
    )
    .build()

  implicit lazy val ec: ExecutionContext = injected[ExecutionContext]

  def sut: AddressLookupConnector = injected[AddressLookupConnector]

  "AddressLookupConnector" when {

    "initJourney is called" must {

      val url = "/api/init"
      val redirectUrl = "/foo"

      "return ACCEPTED (202) with the redirect url in the Location header" in {

        server.stubFor(
          post(urlEqualTo(url)).willReturn(aResponse().withStatus(ACCEPTED).withHeader("Location", redirectUrl))
        )

        val result = await(sut.initJourney)

        result.status shouldBe ACCEPTED
        result.header("Location") shouldBe Some(redirectUrl)
      }

      "return a 4xx response" in {

        server.stubFor(
          post(urlEqualTo(url)).willReturn(badRequest())
        )

        val result = await(sut.initJourney)

        result.status shouldBe BAD_REQUEST
      }

      "return a 5xx response" when {

        "the http call returns 5xx" in {

          server.stubFor(
            post(urlEqualTo(url)).willReturn(serverError())
          )

          val result = await(sut.initJourney)

          result.status shouldBe INTERNAL_SERVER_ERROR
        }

        "an exception is thrown" in {

          server.stubFor(
            post(urlEqualTo(url)).willReturn(aResponse().withFault(Fault.MALFORMED_RESPONSE_CHUNK))
          )

          val result = await(sut.initJourney)

          result.status shouldBe INTERNAL_SERVER_ERROR
        }
      }
    }

    "getConfirmedAddress is called" must {

      val id = UUID.randomUUID().toString
      val url = s"/api/confirmed?id=$id"

      "return OK (200) with the address in the response" in {

        val response = """{
                         |    "auditRef" : "bed4bd24-72da-42a7-9338-f43431b7ed72",
                         |    "id" : "GB990091234524",
                         |    "address" : {
                         |        "lines" : [ "10 Other Place", "Some District", "Anytown" ],
                         |        "postcode" : "ZZ1 1ZZ",
                         |        "country" : {
                         |            "code" : "GB",
                         |            "name" : "United Kingdom"
                         |        }
                         |    }
                         |}""".stripMargin

        server.stubFor(
          get(urlEqualTo(url)).willReturn(ok().withBody(response))
        )

        val result = await(sut.getConfirmedAddress(id))

        result.status shouldBe OK
        result.json shouldBe Json.parse(response)
      }

      "return a 4xx response" in {

        server.stubFor(
          get(urlEqualTo(url)).willReturn(notFound())
        )

        val result = await(sut.getConfirmedAddress(id))

        result.status shouldBe NOT_FOUND
      }

      "return a 5xx response" when {

        "the http call returns 5xx" in {

          server.stubFor(
            get(urlEqualTo(url)).willReturn(serverError())
          )

          val result = await(sut.getConfirmedAddress(id))

          result.status shouldBe INTERNAL_SERVER_ERROR
        }

        "an exception is thrown" in {

          server.stubFor(
            get(urlEqualTo(url)).willReturn(aResponse().withFault(Fault.MALFORMED_RESPONSE_CHUNK))
          )

          val result = await(sut.getConfirmedAddress(id))

          result.status shouldBe INTERNAL_SERVER_ERROR
        }
      }
    }
  }
}

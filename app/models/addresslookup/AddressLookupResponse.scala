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

package models.addresslookup

import play.api.libs.json.Json
import uk.gov.hmrc.http.HttpResponse

sealed trait AddressLookupResponse

final case class AddressLookupSuccessResponse(addressList: RecordSet) extends AddressLookupResponse
final case class AddressLookupUnexpectedResponse(r: HttpResponse) extends AddressLookupResponse
final case class AddressLookupErrorResponse(cause: Exception) extends AddressLookupResponse

final case class AddressV2(lines: Seq[String], postcode: String, country: Country) {
  override def toString: String = s"${lines.mkString(", ")}, $postcode, ${country.name}"
}

object AddressV2 {
  implicit val reads = Json.reads[AddressV2]
}

final case class AddressLookupResponseV2(id: Option[String], address: AddressV2)

object AddressLookupResponseV2 {
  implicit val reads = Json.reads[AddressLookupResponseV2]
}

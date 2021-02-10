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

import config.ConfigDecorator
import play.api.libs.json.{Json, OWrites}
import uk.gov.hmrc.play.binders.Origin

final case class ConfigOptions(
  continueUrl: String,
  homeNavHref: String,
  signOutHref: String
)

object ConfigOptions {
  implicit val writes: OWrites[ConfigOptions] = Json.writes[ConfigOptions]
}

final case class AddressLookupConfig(version: Int = 2, options: ConfigOptions)

object AddressLookupConfig {
  implicit val writes: OWrites[AddressLookupConfig] = Json.writes[AddressLookupConfig]

  def default(configDecorator: ConfigDecorator): AddressLookupConfig =
    this.apply(
      2,
      ConfigOptions(
        configDecorator.addressLookupContinueUrl,
        configDecorator.pertaxFrontendHomeUrl,
        configDecorator.getBasGatewayFrontendSignOutUrl(
          configDecorator.getFeedbackSurveyUrl(configDecorator.defaultOrigin))
      )
    )
}

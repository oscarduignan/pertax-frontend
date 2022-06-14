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

package models.admin

import play.api.libs.functional.syntax._
import play.api.libs.json._
import play.api.mvc.PathBindable

sealed trait FeatureFlag {
  def name: FeatureFlagName

  def isEnabled: Boolean

  def isDisabled: Boolean = !isEnabled
}

sealed trait FeatureFlagName {
  def asString: String
}

object FeatureFlagName {

  case object OnlinePaymentIntegration extends FeatureFlagName {
    val asString = "online-payment-integration"
  }

  val flags = Seq(
    OnlinePaymentIntegration
  )

  implicit val reads: Reads[FeatureFlagName] = Reads {
    case JsString(OnlinePaymentIntegration.asString) =>
      JsSuccess(OnlinePaymentIntegration)
    case _ =>
      JsError("Unrecognised feature flag name")
  }

  implicit val writes: Writes[FeatureFlagName] =
    Writes(value => JsString(value.asString))

  implicit def pathBindable: PathBindable[FeatureFlagName] = new PathBindable[FeatureFlagName] {

    override def bind(key: String, value: String): Either[String, FeatureFlagName] =
      JsString(value).validate[FeatureFlagName] match {
        case JsSuccess(name, _) =>
          Right(name)
        case _ =>
          Left("invalid feature flag name")
      }

    override def unbind(key: String, value: FeatureFlagName): String =
      value.asString
  }

  implicit val formats: Format[FeatureFlagName] =
    Format(reads, writes)
}

object FeatureFlag {

  case class Enabled(name: FeatureFlagName) extends FeatureFlag {
    val isEnabled = true
  }

  case class Disabled(name: FeatureFlagName) extends FeatureFlag {
    val isEnabled = false
  }

  def apply(name: FeatureFlagName, enabled: Boolean): FeatureFlag =
    if (enabled) Enabled(name)
    else Disabled(name)

  implicit val reads: Reads[FeatureFlag] =
    (__ \ "isEnabled").read[Boolean].flatMap {
      case true =>
        (__ \ "name")
          .read[FeatureFlagName]
          .map(Enabled(_).asInstanceOf[FeatureFlag])
      case false =>
        (__ \ "name")
          .read[FeatureFlagName]
          .map(Disabled(_).asInstanceOf[FeatureFlag])
    }

  implicit val writes: Writes[FeatureFlag] =
    (
      (__ \ "name").write[FeatureFlagName] and
        (__ \ "isEnabled").write[Boolean]
    ).apply(ff => (ff.name, ff.isEnabled))

  implicit val formats: Format[FeatureFlag] =
    Format(reads, writes)
}

case class FeatureFlags(flags: Seq[FeatureFlag])

object FeatureFlagMongoFormats {
  implicit val formats: Format[FeatureFlags] =
    Json.format[FeatureFlags]
}

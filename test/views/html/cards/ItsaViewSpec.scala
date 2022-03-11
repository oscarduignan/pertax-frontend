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

package views.html.cards
import config.ConfigDecorator
import play.api.i18n.Messages
import views.html.ViewSpec
import views.html.cards.home.ItsaView

class ItsaViewSpec extends ViewSpec {

  val itsaView = injected[ItsaView]
  implicit val configDecorator: ConfigDecorator = injected[ConfigDecorator]

  val nextDeadlineTaxYear = "2021"

  "Itsa card" must {

    val doc =
      asDocument(
        itsaView(nextDeadlineTaxYear).toString
      )

    "render the given heading correctly" in {

      doc.text() must include(
        Messages("label.self_assessment")
      )
    }

    "render the given content correctly" in {

      doc.text() must include(
        Messages("label.view_and_manage_your_self_assessment_tax_return_the_deadline_for_online_", nextDeadlineTaxYear)
      )
    }
  }
}

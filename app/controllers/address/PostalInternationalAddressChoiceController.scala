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

package controllers.address

import com.google.inject.Inject
import config.ConfigDecorator
import controllers.auth.AuthJourney
import controllers.bindable.PostalAddrType
import controllers.controllershelpers.AddressJourneyCachingHelper
import models.SubmittedInternationalAddressChoiceId
import models.dto.InternationalAddressChoiceDto
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import views.html.interstitial.DisplayAddressInterstitialView
import views.html.personaldetails.PostalInternationalAddressChoiceView

import scala.concurrent.{ExecutionContext, Future}

class PostalDoYouLiveInTheUKController @Inject() (
  cachingHelper: AddressJourneyCachingHelper,
  authJourney: AuthJourney,
  cc: MessagesControllerComponents,
  postalInternationalAddressChoiceView: PostalInternationalAddressChoiceView,
  displayAddressInterstitialView: DisplayAddressInterstitialView
)(implicit configDecorator: ConfigDecorator, ec: ExecutionContext)
    extends AddressController(authJourney, cc, displayAddressInterstitialView) {

  def onPageLoad: Action[AnyContent] =
    authenticate.async { implicit request =>
      addressJourneyEnforcer { _ => _ =>
        cachingHelper.gettingCachedAddressPageVisitedDto { addressPageVisitedDto =>
          cachingHelper.enforceDisplayAddressPageVisited(addressPageVisitedDto) {
            Future.successful(
              Ok(
                postalInternationalAddressChoiceView(
                  InternationalAddressChoiceDto.form(Some("error.postal_address_uk_select"))
                )
              )
            )
          }
        }
      }
    }

  def onSubmit: Action[AnyContent] =
    authenticate.async { implicit request =>
      addressJourneyEnforcer { _ => _ =>
        InternationalAddressChoiceDto
          .form(Some("error.postal_address_uk_select"))
          .bindFromRequest
          .fold(
            formWithErrors => Future.successful(BadRequest(postalInternationalAddressChoiceView(formWithErrors))),
            internationalAddressChoiceDto =>
              cachingHelper.addToCache(SubmittedInternationalAddressChoiceId, internationalAddressChoiceDto) map { _ =>
                if (internationalAddressChoiceDto.value) {
                  Redirect(routes.PostcodeLookupController.onPageLoad(PostalAddrType))
                } else {
                  if (configDecorator.updateInternationalAddressInPta) {
                    Redirect(routes.UpdateInternationalAddressController.onPageLoad(PostalAddrType))
                  } else {
                    Redirect(routes.AddressErrorController.cannotUseThisService(PostalAddrType))
                  }
                }
              }
          )

      }
    }
}

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

package controllers.address

import com.google.inject.Inject
import config.ConfigDecorator
import connectors.TaxCreditsConnector
import controllers.auth.{AuthJourney, WithActiveTabAction}
import controllers.controllershelpers.{AddressJourneyCachingHelper, PersonalDetailsCardGenerator}
import models.{AddressJourneyTTLModel, AddressPageVisitedDtoId}
import play.api.http.Status.OK
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.EditAddressLockRepository
import services.{NinoDisplayService, TaxCreditsService}
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.renderer.TemplateRenderer
import util.AuditServiceTools.buildPersonDetailsEvent
import viewmodels.PersonalDetailsViewModel
import views.html.interstitial.DisplayAddressInterstitialView
import views.html.personaldetails.PersonalDetailsView

import java.util.concurrent.TimeUnit
import scala.concurrent.duration.{Duration, SECONDS}
import scala.concurrent.{Await, CanAwait, ExecutionContext, Future}

class PersonalDetailsController @Inject() (
  val personalDetailsCardGenerator: PersonalDetailsCardGenerator,
  val personalDetailsViewModel: PersonalDetailsViewModel,
  val editAddressLockRepository: EditAddressLockRepository,
  ninoDisplayService: NinoDisplayService,
  taxCreditsService: TaxCreditsService,
  authJourney: AuthJourney,
  cachingHelper: AddressJourneyCachingHelper,
  withActiveTabAction: WithActiveTabAction,
  auditConnector: AuditConnector,
  cc: MessagesControllerComponents,
  displayAddressInterstitialView: DisplayAddressInterstitialView,
  personalDetailsView: PersonalDetailsView
)(implicit
  configDecorator: ConfigDecorator,
  templateRenderer: TemplateRenderer,
  ec: ExecutionContext
) extends AddressController(authJourney, withActiveTabAction, cc, displayAddressInterstitialView) {

  def redirectToYourProfile: Action[AnyContent] = authenticate.async { _ =>
    Future.successful(Redirect(controllers.address.routes.PersonalDetailsController.onPageLoad()))
  }

  def onPageLoad: Action[AnyContent] =
    authenticate.async { implicit request =>
      import models.dto.AddressPageVisitedDto

      for {
        addressModel <- request.nino
                          .map { nino =>
                            editAddressLockRepository.get(nino.withoutSuffix)
                          }
                          .getOrElse(
                            Future.successful(List[AddressJourneyTTLModel]())
                          )
        ninoToDisplay       <- ninoDisplayService.getNino
        taxCreditsAvailable <- taxCreditsService.checkForTaxCredits(ninoToDisplay)

        _ <- request.personDetails
               .map { details =>
                 auditConnector.sendEvent(
                   buildPersonDetailsEvent(
                     "personalDetailsPageLinkClicked",
                     details
                   )
                 )
               }
               .getOrElse(Future.successful(Unit))
        _ <- cachingHelper
               .addToCache(AddressPageVisitedDtoId, AddressPageVisitedDto(true))

      } yield {
        val personalDetails = personalDetailsViewModel
          .getPersonDetailsTable(ninoToDisplay)
        val addressDetails = personalDetailsViewModel.getAddressRow(addressModel, taxCreditsAvailable)
        val trustedHelpers = personalDetailsViewModel.getTrustedHelpersRow
        val paperlessHelpers = personalDetailsViewModel.getPaperlessSettingsRow
        val signinDetailsHelpers = personalDetailsViewModel.getSignInDetailsRow
        Ok(
          personalDetailsView(
            personalDetails,
            addressDetails,
            trustedHelpers,
            paperlessHelpers,
            signinDetailsHelpers,
            taxCreditsAvailable
          )
        )
      }
    }
}

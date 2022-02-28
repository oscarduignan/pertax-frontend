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

package controllers.controllershelpers

import cats.data.OptionT
import cats.instances.future._
import com.google.inject.Inject
import config.ConfigDecorator
import controllers.PertaxBaseController
import controllers.auth.requests.UserRequest
import models.AddressJourneyTTLModel
import play.api.Logging
import play.api.mvc.{MessagesControllerComponents, Result}
import repositories.EditAddressLockRepository
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.renderer.TemplateRenderer

import scala.concurrent.{ExecutionContext, Future}

class RlsInterruptHelper @Inject() (
  cc: MessagesControllerComponents,
  editAddressLockRepository: EditAddressLockRepository
)(implicit ec: ExecutionContext, templateRenderer: TemplateRenderer)
    extends PertaxBaseController(cc) with Logging {

  def enforceByRlsStatus(
    block: => Future[Result]
  )(implicit
    request: UserRequest[_],
    hc: HeaderCarrier,
    ec: ExecutionContext,
    configDecorator: ConfigDecorator
  ): Future[Result] =
    if (configDecorator.rlsInterruptToggle) {
      logger.info("Check for RLS interrupt")
      (for {
        personDetails <- OptionT.fromOption(request.personDetails)
        nino          <- OptionT.fromOption(request.nino)
        editAddressLockRepository: List[AddressJourneyTTLModel] <-
          OptionT.liftF(editAddressLockRepository.get(nino.withoutSuffix))
      } yield {
        val residentialLock = editAddressLockRepository.exists(_.editedAddress.addressType == "EditResidentialAddress")
        val correspondenceLock =
          editAddressLockRepository.exists(_.editedAddress.addressType == "EditCorrespondenceAddress")
        logger.warn("Residential lock: " + residentialLock.toString)
        logger.warn("Correspondence lock: " + correspondenceLock.toString)
        logger.warn("Residential address rls: " + personDetails.address.exists(_.isRls))
        logger.warn("Correspondence address rls: " + personDetails.correspondenceAddress.exists(_.isRls))

        if (personDetails.address.exists(_.isRls) && !residentialLock)
          Future.successful(Redirect(controllers.routes.RlsController.rlsInterruptOnPageLoad(true)))
        else if (personDetails.correspondenceAddress.exists(_.isRls) && !correspondenceLock)
          Future.successful(Redirect(controllers.routes.RlsController.rlsInterruptOnPageLoad(false)))
        else
          block
      }).getOrElse(block).flatten
    } else {
      block
    }
}

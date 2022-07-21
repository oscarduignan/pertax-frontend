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

package controllers.admin

import controllers.auth.InternalAuthAction
import models.admin.FeatureFlagName
import play.api.libs.json.{JsBoolean, Json}
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}
import services.admin.FeatureFlagService

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class FeatureFlagController @Inject() (
  val auth: InternalAuthAction,
  ffService: FeatureFlagService,
  cc: ControllerComponents
)(implicit ec: ExecutionContext)
    extends AbstractController(cc) {

  def get(): Action[AnyContent] =
    auth().async {
      ffService.getAll
        .map(flags => Ok(Json.toJson(flags)))
    }

  def put(flagName: FeatureFlagName): Action[AnyContent] =
    auth().async { request =>
      request.body.asJson match {
        case Some(JsBoolean(enabled)) =>
          ffService
            .set(flagName, enabled)
            .map(_ => NoContent)
        case _ =>
          Future.successful(BadRequest)
      }
    }
}

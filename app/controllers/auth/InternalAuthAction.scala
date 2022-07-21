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

package controllers.auth

import _root_.config.ConfigDecorator
import com.google.inject.Inject
import uk.gov.hmrc.internalauth.client.Predicate.Permission
import uk.gov.hmrc.internalauth.client._

import scala.concurrent.ExecutionContext

class InternalAuthAction @Inject() (
  config: ConfigDecorator,
  internalAuth: BackendAuthComponents
)(implicit
  val executionContext: ExecutionContext
) {
  private val permission: Permission =
    Permission(
      resource = Resource(
        resourceType = ResourceType(config.internalAuthResourceType),
        resourceLocation = ResourceLocation("*")
      ),
      action = IAAction("ADMIN")
    )

  def apply() =
    internalAuth.authorizedAction[Unit](permission)
}

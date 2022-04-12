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

package controllers

import akka.stream.scaladsl.StreamConverters
import com.google.inject.Inject
import org.apache.fop.apps.FopFactory
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import views.xml.xml.NiLetterView

import java.io._
import javax.xml.transform.TransformerFactory
import javax.xml.transform.sax.SAXResult
import javax.xml.transform.stream.StreamSource

class TestController @Inject() (val cc: MessagesControllerComponents, template: NiLetterView)
    extends FrontendController(cc) with I18nSupport {

  def onDownload: Action[AnyContent] = Action { implicit request =>
    val byteArrayPdfStream = new ByteArrayOutputStream()
    val bufferedPdfStream = new BufferedOutputStream(byteArrayPdfStream)
    val fopFactory = FopFactory.newInstance(new File("./conf/resources/fop/fop.xconf"))
    val fop = fopFactory.newFop("application/pdf", bufferedPdfStream)

    val factory = TransformerFactory.newInstance
    val res = new SAXResult(fop.getDefaultHandler)
    val src = new StreamSource(new StringReader(template.render(implicitly).toString()))

    factory.newTransformer.transform(src, res)

    bufferedPdfStream.close()
    Ok.chunked(
      StreamConverters.fromInputStream(() => new ByteArrayInputStream(byteArrayPdfStream.toByteArray)),
      Some("application/pdf")
    ).withHeaders(
      CONTENT_DISPOSITION -> s"inline; filename = test.pdf"
    )
  }
}

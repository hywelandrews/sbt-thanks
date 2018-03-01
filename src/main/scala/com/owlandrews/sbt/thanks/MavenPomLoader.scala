/*
 * Copyright (c) 2018 Hywel Andrews
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.owlandrews.sbt.thanks

import cats.effect.Effect
import sbt.librarymanagement.{ MavenRepository, ModuleID }

import scala.language.postfixOps
import scala.xml.XML

class MavenPomLoader[F[_]: Effect](repo: MavenRepository, cross: Option[String => String], downloader: Downloader[F]) {

  import cats.implicits._

  def getScm(module: ModuleID): F[Scm] =
    downloadXML(pomUrl(module)).map(extractScm)

  private def pomUrl(module: ModuleID) = {
    val nameOptionalCrossVersion = cross.map(f => f(module.name)).getOrElse(module.name)
    artifactUrl(nameOptionalCrossVersion, module, repo) + "/" + module.revision + "/" + nameOptionalCrossVersion + "-" + module.revision + ".pom"
  }

  private def artifactUrl(name: String, module: ModuleID, repo: MavenRepository) =
    (module.organization.split('.') :+ name foldLeft repo.root.stripSuffix("/"))(_ + '/' + _)

  private def extractScm(metadata: xml.Elem): Scm =
    Scm(metadata \ "scm" \ "url" text, metadata \ "scm" \ "connection" text, metadata \ "organization" \ "name" text)

  private def downloadXML(url: String) =
    downloader.startDownloadWithHttp(url).map(XML.load)
}

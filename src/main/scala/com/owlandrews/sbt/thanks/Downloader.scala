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

import java.io.{ ByteArrayInputStream, InputStream }
import java.net.URL

import cats.effect.Effect
import org.http4s.Uri
import org.http4s.client.Client
import sbt.librarymanagement.ivy.Credentials
import sbt.util.Logger

class Downloader[F[_]: Effect](httpClient: Client[F], credentials: Seq[Credentials], logger: Logger) {

  def startDownload(url: URL): InputStream = {
    val hostCredentials = Credentials.forHost(credentials, url.getHost)
    val connection      = url.openConnection()
    hostCredentials match {
      case Some(c) =>
        logger.debug(s"Downloading $url as ${c.userName}")
      //val auth = Base64.encodeToString(s"${c.userName}:${c.passwd}".getBytes)
      //connection.setRequestProperty("Authorization", s"Basic $auth")
      case None =>
        logger.debug(s"Downloading $url anonymously")
    }
    connection.getInputStream
  }

  def startDownloadWithHttp(url: String): F[InputStream] = {
    val target =
      Uri.fromString(url).toOption
    val user = target.map(_.userInfo)

    user match {
      case Some(c) =>
        logger.debug(s"Downloading $url as $c")
      //val auth = Base64.encodeToString(s"${c.userName}:${c.passwd}".getBytes)
      //connection.setRequestProperty("Authorization", s"Basic $auth")
      case None =>
        logger.debug(s"Downloading $url anonymously")
    }

    import cats.implicits._
    httpClient.expect[Array[Byte]](url).map(new ByteArrayInputStream(_))
  }

}

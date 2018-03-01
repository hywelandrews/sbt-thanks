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
import org.http4s.client.Client
import org.http4s.headers.{ Authorization, `Content-Length` }
import org.http4s.util.CaseInsensitiveString
import org.http4s.{ BasicCredentials, Headers, Method, Request, Uri }

class GitHubClient[F[_]: Effect](httpClient: Client[F])(username: String, token: String) {

  private val githubUri                = "https://api.github.com"
  private val basicAuthorizationHeader = Headers(Authorization(BasicCredentials(username, token)))
  private val zeroContentLengthHeader  = Headers(`Content-Length`.unsafeFromLong(0))

  private def starredUri(repo: String) = {
    val path = s"$githubUri/user/starred/$repo"
    Uri
      .fromString(path)
      .toOption
      .getOrElse(throw new IllegalArgumentException(s"Malformed URI $path"))
  }

  private def userUri = {
    val path = s"$githubUri/user"
    Uri
      .fromString(path)
      .toOption
      .getOrElse(throw new IllegalArgumentException(s"Malformed URI $path"))
  }

  import cats.implicits._

  def supportsPublicRepo: F[Boolean] = {
    import cats.effect._
    val path    = userUri
    val request = Request[F](method = Method.GET, uri = path, headers = basicAuthorizationHeader)
    httpClient.fetch(request) { f =>
      Effect[F].pure(f.headers.exists { header =>
        header.name == CaseInsensitiveString("X-OAuth-Scopes") && header.value.contains("public_repo")
      })
    }
  }

  def starred(repo: String): F[Boolean] = {
    val path    = starredUri(repo)
    val request = Request[F](method = Method.GET, uri = path, headers = basicAuthorizationHeader)
    httpClient.status(request).map(s => if (s.code == 204) true else false)
  }

  def star(repo: String): F[Boolean] = {
    val path = starredUri(repo)
    val request =
      Request[F](method = Method.PUT, uri = path, headers = basicAuthorizationHeader ++ zeroContentLengthHeader)
    httpClient
      .status(request)
      .map(s => if (s.code == 204) true else false)
  }

  def get(name: String): F[String] = {
    val target =
      Uri
        .fromString(s"$githubUri/$name")
        .toOption
        .getOrElse(throw new IllegalArgumentException(s"Malformed URI $githubUri/$name"))
    val request = Request[F](method = Method.GET, uri = target, headers = basicAuthorizationHeader)
    httpClient.expect[String](request)
  }
}

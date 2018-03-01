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

sealed class Scm(url: String, connection: String, organization: String, path: String)

case class GitHubScm(url: String, connection: String, organization: String, path: String, star: GitHubStar)
    extends Scm(url, connection, organization, path)

case class GitHubStar(given: Boolean, gave: Boolean)

object Scm {
  private val gitRegEx          = "\\.git\\b".r
  private val gitHubRegEx       = "\\//github.com/\\b".r
  private val defaultGitHubStar = GitHubStar(given = false, gave = false)

  private def isGitHubInternal(connection: String, url: String) =
    connection.startsWith("scm:git:git@github.com") ||
    connection.startsWith("scm:git:https://github.com/") ||
    url.startsWith("https://github.com/")

  def apply(url: String, connection: String, organization: String): Scm =
    if (isGitHubInternal(connection, url)) {
      connection.split(':') match {
        case Array(_, _, "git@github.com", path) =>
          val urlNoGit          = gitRegEx.replaceAllIn(url, "")
          val pathNoGitNoGitHub = gitHubRegEx.replaceAllIn(gitRegEx.replaceAllIn(path, ""), "")
          GitHubScm(url = s"https://github.com/$urlNoGit",
                    connection,
                    organization,
                    pathNoGitNoGitHub,
                    defaultGitHubStar)
        case Array(_, _, "git", path) =>
          val urlNoGit          = gitRegEx.replaceAllIn(url, "")
          val pathNoGitNoGitHub = gitHubRegEx.replaceAllIn(gitRegEx.replaceAllIn(path, ""), "")
          GitHubScm(url = urlNoGit, connection, organization, pathNoGitNoGitHub, defaultGitHubStar)
        case Array(_, _, "https", path) =>
          val urlNoGit          = gitRegEx.replaceAllIn(url, "")
          val pathNoGitNoGitHub = gitHubRegEx.replaceAllIn(gitRegEx.replaceAllIn(path, ""), "")
          GitHubScm(url = urlNoGit, connection, organization, pathNoGitNoGitHub, defaultGitHubStar)
      }
    } else new Scm(url, connection, organization, connection)
}

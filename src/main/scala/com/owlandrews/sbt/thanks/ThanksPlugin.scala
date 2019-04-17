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

import cats.effect.{ ContextShift, IO }
import sbt.Keys.{ libraryDependencies, scalaBinaryVersion, _ }
import sbt.{ Def, _ }

import scala.util.control.NonFatal

object ThanksPlugin extends AutoPlugin {
  // by defining autoImport, the settings are automatically imported into user's `*.sbt`
  object autoImport {
    // configuration points, like the built-in `version`, `libraryDependencies`, or `compile`
    lazy val thanks = taskKey[Unit]("Show dependencies which can be starred on github.")
    lazy val thanksPublicGitHubApiUser =
      settingKey[String]("GitHub.com API User used for automatically starring repos.")
    lazy val thanksPublicGitHubApiKey = settingKey[String]("GitHub.com API Key used for automatically starring repos.")
  }

  import autoImport._
  // default values for the tasks and settings
  override val projectSettings: Seq[Def.Setting[_]] = Seq(
    thanks := {
      Thanks(
        libraryDependencies.value,
        fullResolvers.value,
        scalaVersion.value,
        scalaBinaryVersion.value,
        thanksPublicGitHubApiUser.value,
        thanksPublicGitHubApiKey.value
      )
    },
    thanksPublicGitHubApiKey := Thanks.defaultPublicGitHubApiKey,
    thanksPublicGitHubApiUser := Thanks.defaultPublicGitHubApiUser
  )

  override def trigger = allRequirements
}

object Thanks {

  import cats.implicits._

  lazy val logger = ConsoleLogger()

  val defaultPublicGitHubApiKey  = "NoPublicGitHubApiKey"
  val defaultPublicGitHubApiUser = "NoPublicGitHubApiUser"

  def apply(dependencies: Seq[ModuleID],
            resolvers: Seq[Resolver],
            scalaVersion: String,
            scalaBinaryVersion: String,
            gitHubPublicApiUser: String,
            gitHubPublicApiKey: String): Unit = {

    import scala.concurrent.ExecutionContext.Implicits.global
    import org.http4s.client.blaze._

    implicit val cs: ContextShift[IO] = IO.contextShift(global)

    BlazeClientBuilder[IO](global).resource
      .use { httpClient =>
        val downloader     = new Downloader[IO](httpClient, Seq.empty, logger)
        val mavenResolvers = new MavenRepositories(resolvers)
        val githubClient   = new GitHubClient[IO](httpClient)(gitHubPublicApiUser, gitHubPublicApiKey)

        val thanksProgram = dependencies.toVector.map { dependency =>
          val getPom =
            new MavenPomLoader[IO](mavenResolvers.public,
                                   CrossVersion(dependency.crossVersion, scalaVersion, scalaBinaryVersion),
                                   downloader)

          getPom
            .getScm(dependency)
            .attempt
            .flatMap(
              getPomFromAllResolvers(_)(dependency, mavenResolvers.all, downloader, scalaVersion, scalaBinaryVersion)
            )
            .flatMap(processGitHubStar(_)(githubClient))
            .map(reportGitHubStarProcessing(_)(dependency))
            .recover {
              case NonFatal(_) => logger.warn(s"🛑  Unable to thank ${dependency.name}")
            }
        }

        val program = for {
          hasPublicRepoSupport <- githubClient.supportsPublicRepo
          noPublicRepoSupport = IO(
            logger.warn(
              """GitHub API Key does not support public_repo's:
              |This can be updated in Settings -> Developer settings -> Personal access tokens -> repo -> public_repo
              |""".stripMargin
            )
          )
          result <- if (hasPublicRepoSupport) thanksProgram.sequence[IO, Unit] else noPublicRepoSupport
        } yield result

        program
      }
      .unsafeRunSync()

  }

  private def reportGitHubStarProcessing(scm: Scm)(module: ModuleID): Unit = scm match {
    case github @ GitHubScm(_, _, _, _, GitHubStar(true, false)) =>
      logger.info(
        s"✨  Already thanked ${github.organization} for ${module.name}"
      )
    case github @ GitHubScm(_, _, _, _, GitHubStar(false, true)) =>
      logger.info(
        s"🌟  Thanked ${github.organization} for ${module.name}"
      )
    case github @ GitHubScm(_, _, _, _, GitHubStar(false, false)) =>
      logger.info(
        s"🛑  Unable to thank ${github.organization} for ${module.name}"
      )
    case _: Scm => // TODO: Handel more than just github?
  }

  private def processGitHubStar(scm: Scm)(githubClient: GitHubClient[IO]): IO[Scm] = scm match {
    case github: GitHubScm =>
      for {
        givenStar <- githubClient.starred(github.path)
        gaveStar  <- if (!givenStar) githubClient.star(github.path) else IO.pure(false)
      } yield github.copy(star = GitHubStar(givenStar, gaveStar))
    case r: Scm => IO(r)
  }

  private def getPomFromAllResolvers(
      in: Either[Throwable, Scm]
  )(module: ModuleID,
    mavenResolvers: Seq[MavenRepository],
    downloader: Downloader[IO],
    scalaVersion: String,
    scalaBinaryVersion: String): IO[Scm] = in match {
    case Left(_) =>
      val alternateResolvers = mavenResolvers
        .map { resolve =>
          val loader =
            new MavenPomLoader(resolve, CrossVersion(module.crossVersion, scalaVersion, scalaBinaryVersion), downloader)

          loader
            .getScm(module)
            .attempt
            .map(_.toOption)
        }
        .toVector
        .sequence[IO, Option[Scm]]

      alternateResolvers.map { r =>
        r.collectFirst { case Some(t) => t }
          .getOrElse(throw new Exception(s"Could not find ${module.name} from any resolver"))
      }
    case Right(r) => IO(r)
  }
}

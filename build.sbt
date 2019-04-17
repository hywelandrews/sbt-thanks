// *****************************************************************************
// Projects
// *****************************************************************************

lazy val `sbt-thanks` =
  project
    .in(file("."))
    .enablePlugins(AutomateHeaderPlugin, GitVersioning)
    .settings(settings)
    .settings(
      libraryDependencies ++= Seq(
        library.http4sDsl,
        library.http4sClient,

        library.scalaCheck % Test,
        library.scalaTest  % Test
      )
    )

// *****************************************************************************
// Library dependencies
// *****************************************************************************

lazy val library =
  new {
    object Version {
      val scalaCheck    = "1.14.0"
      val scalaTest     = "3.0.7"
      val http4sVersion = "0.19.0"
    }
    val http4sDsl    = "org.http4s"     %% "http4s-dsl"    % Version.http4sVersion
    val http4sClient = "org.http4s"     %% "http4s-blaze-client" % Version.http4sVersion
    val scalaCheck   = "org.scalacheck" %% "scalacheck"    % Version.scalaCheck
    val scalaTest    = "org.scalatest"  %% "scalatest"     % Version.scalaTest
  }

// *****************************************************************************
// Settings
// *****************************************************************************

lazy val settings =
commonSettings ++
gitSettings ++
scalafmtSettings

lazy val commonSettings =
  Seq(
    name := "sbt-thanks",
    version := "0.3.0",
    isSnapshot := false,
    scalaVersion := "2.12.8",
    organization := "com.owlandrews",
    organizationName := "Hywel Andrews",
    sbtPlugin := true,
    startYear := Some(2018),
    licenses += ("MIT", url("https://opensource.org/licenses/MIT")),
    scalacOptions ++= Seq(
      "-unchecked",
      "-deprecation",
      "-Ypartial-unification",
      "-language:_",
      "-target:jvm-1.8",
      "-encoding",
      "UTF-8"
    ),
    unmanagedSourceDirectories.in(Compile) := Seq(scalaSource.in(Compile).value),
    unmanagedSourceDirectories.in(Test) := Seq(scalaSource.in(Test).value)
  )

lazy val gitSettings =
  Seq(
    git.useGitDescribe := true
  )

lazy val scalafmtSettings =
  Seq(
    scalafmtOnCompile := true,
    scalafmtOnCompile.in(Sbt) := false,
    scalafmtVersion := "1.3.0"
  )

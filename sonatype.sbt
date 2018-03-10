import xerial.sbt.Sonatype._

publishMavenStyle := true

sonatypeProfileName := "com.owlandrews"
sonatypeProjectHosting := Some(GitHubHosting(user="hywelandrews", repository="sbt-thanks", email="hywel@team16.co.uk"))
developers := List(
  Developer(id = "owlandrews", name = "Hywel Andrews", email = "hywel@team16.co.uk", url = url("http://owlandrews.com"))
)

publishTo := Some(
  if (isSnapshot.value)
    Opts.resolver.sonatypeSnapshots
  else
    Opts.resolver.sonatypeStaging
)
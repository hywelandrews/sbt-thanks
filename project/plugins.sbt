addSbtPlugin("com.lucidchart"    % "sbt-scalafmt" % "1.16")
addSbtPlugin("com.typesafe.sbt"  % "sbt-git"      % "1.0.0")
addSbtPlugin("de.heikoseeberger" % "sbt-header"   % "5.2.0")
addSbtPlugin("org.xerial.sbt"    % "sbt-sonatype" % "2.5")

libraryDependencies += "org.slf4j" % "slf4j-nop" % "1.7.25" // Needed by sbt-git

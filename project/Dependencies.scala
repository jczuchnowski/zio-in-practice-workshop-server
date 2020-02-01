import sbt._

object Dependencies {
  lazy val zio    = "dev.zio" %% "zio"     % "1.0.0-RC17"
  lazy val zioNio = "dev.zio" %% "zio-nio" % "1.0.0-RC2"
  lazy val zioStreams    = "dev.zio" %% "zio-streams"     % "1.0.0-RC17"
}

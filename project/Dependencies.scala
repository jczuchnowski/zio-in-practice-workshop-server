import sbt._

object Dependencies {
  lazy val zio    = "dev.zio" %% "zio"     % "1.0.0-RC17"
  lazy val zioNio = "dev.zio" %% "zio-nio" % "0.4.0+10-c40ec920+20200114-0201"
  lazy val zioStreams    = "dev.zio" %% "zio-streams"     % "1.0.0-RC17"
}

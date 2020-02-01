import Dependencies._

ThisBuild / scalaVersion     := "2.13.1"
ThisBuild / version          := "0.1.0-SNAPSHOT"
ThisBuild / organization     := "com.github.jczuchnowski"

lazy val root = (project in file("."))
  .settings(
    name := "zio-nio-server",
    libraryDependencies ++= Seq(
      zio,
      zioNio,
      zioStreams
    )
  )

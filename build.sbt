ThisBuild / scalaVersion := "2.13.14"
ThisBuild / version := "0.1.0"

lazy val root = (project in file("."))
  .settings(
    name := "flagsync",
    libraryDependencies ++= Seq(
      "io.circe" %% "circe-core" % "0.14.6",
      "io.circe" %% "circe-generic" % "0.14.6",
      "io.circe" %% "circe-parser" % "0.14.6",
      "org.scalatest" %% "scalatest" % "3.2.18" % Test
    )
  )

ThisBuild / scalaVersion := "2.13.14"
ThisBuild / version := "0.1.0"

lazy val root = (project in file("."))
  .settings(
    name := "room-booking",
    libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.19" % Test
  )

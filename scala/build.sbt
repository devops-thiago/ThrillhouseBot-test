ThisBuild / scalaVersion := "2.13.12"
ThisBuild / version := "0.1.0"

lazy val root = (project in file("."))
  .settings(
    name := "coldchain",
    libraryDependencies ++= Seq(
      "com.h2database" % "h2" % "2.2.224",
      "org.scalatest" %% "scalatest" % "3.2.17" % Test
    )
  )

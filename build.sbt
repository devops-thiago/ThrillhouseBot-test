ThisBuild / scalaVersion := "2.13.14"

lazy val root = (project in file("."))
  .settings(
    name := "inventory-sync",
    libraryDependencies ++= Seq(
      "io.circe" %% "circe-core" % "0.14.9",
      "io.circe" %% "circe-generic" % "0.14.9",
      "io.circe" %% "circe-parser" % "0.14.9",
      "org.postgresql" % "postgresql" % "42.7.3",
      "org.scalatest" %% "scalatest" % "3.2.19" % Test
    )
  )

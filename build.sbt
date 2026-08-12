name := "event-dedup"
version := "0.1.0"
scalaVersion := "2.13.14"

val circeVersion = "0.14.7"

libraryDependencies ++= Seq(
  "io.circe" %% "circe-core" % circeVersion,
  "io.circe" %% "circe-generic" % circeVersion,
  "io.circe" %% "circe-parser" % circeVersion,
  "org.scalatest" %% "scalatest" % "3.2.18" % Test
)

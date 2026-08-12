name := "dlq-reprocessor"
version := "0.1.0"
scalaVersion := "2.13.12"

libraryDependencies ++= Seq(
  "org.postgresql" % "postgresql" % "42.7.3",
  "org.scalatest" %% "scalatest" % "3.2.18" % Test
)

assembly / assemblyJarName := "dlq-reprocessor.jar"
assembly / mainClass := Some("dlq.Main")

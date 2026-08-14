name := "rotation-auditor"
version := "0.1.0"
scalaVersion := "2.13.18"

libraryDependencies ++= Seq(
  "org.postgresql" % "postgresql" % "42.7.3",
  "org.scalatest" %% "scalatest" % "3.2.18" % Test
)

assembly / assemblyJarName := "rotation-auditor.jar"
assembly / mainClass := Some("rotation.Main")

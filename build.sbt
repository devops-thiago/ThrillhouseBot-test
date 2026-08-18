name := "certguard"
version := "0.1.0"
scalaVersion := "2.13.14"

scalacOptions ++= Seq("-deprecation", "-feature", "-Xlint", "-Ywarn-unused")

libraryDependencies ++= Seq(
  "com.lihaoyi" %% "ujson" % "3.3.1",
  "org.postgresql" % "postgresql" % "42.7.3",
  "org.slf4j" % "slf4j-simple" % "2.0.13",
  "org.scalatest" %% "scalatest" % "3.2.18" % Test
)

assembly / assemblyJarName := "certguard.jar"
assembly / mainClass := Some("certguard.Sentinel")

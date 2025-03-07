import com.typesafe.sbt.packager.docker.Cmd

name := "csvw-check"

organization := "io.github.gss-cogs"
version := "0.0.3"
maintainer := "csvcubed@gsscogs.uk"

scalaVersion := "2.13.16"
scalacOptions ++= Seq("-deprecation", "-feature")
autoCompilerPlugins := true

enablePlugins(JavaAppPackaging)
enablePlugins(UniversalPlugin)
enablePlugins(DockerPlugin)
enablePlugins(AshScriptPlugin)

dockerBaseImage := "eclipse-temurin:23-jre-alpine"
dockerEntrypoint := Seq("/opt/docker/bin/csvw-check")
dockerEnvVars := Map("PATH" -> "$PATH:/opt/docker/bin")
Docker / packageName := "csvw-check"

libraryDependencies += "io.cucumber" %% "cucumber-scala" % "8.26.1" % Test
libraryDependencies += "io.cucumber" % "cucumber-junit" % "7.21.1" % Test
libraryDependencies += "com.novocode" % "junit-interface" % "0.11" % Test
libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.19" % Test
libraryDependencies += "com.github.pathikrit" %% "better-files" % "3.9.2" % Test

libraryDependencies += "org.scala-lang.modules" %% "scala-parser-combinators" % "1.1.2"
libraryDependencies += "io.spray" %% "spray-json" % "1.3.6"
libraryDependencies += "org.apache.jena" % "jena-arq" % "5.3.0"
libraryDependencies += "joda-time" % "joda-time" % "2.13.1"
libraryDependencies += "com.github.scopt" %% "scopt" % "4.1.0"
// Past version 2.6.21 AKKA starts requiring a license key which doesn't fit the use-case of this OSS project at all.
// Unfortunately it looks like we will want to stop using AKKA entirely, which would require quite a bit of work.
libraryDependencies += "com.typesafe.akka" %% "akka-stream" % "2.6.21"
libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.5.16"
libraryDependencies += "com.typesafe.scala-logging" %% "scala-logging" % "3.9.5"
libraryDependencies += "com.fasterxml.jackson.core" % "jackson-databind" % "2.18.2"
libraryDependencies += "com.fasterxml.jackson.core" % "jackson-annotations" % "2.18.2"
libraryDependencies += "com.fasterxml.jackson.core" % "jackson-core" % "2.18.2"
libraryDependencies += "com.softwaremill.sttp.client3" %% "core" % "3.10.3"
libraryDependencies += "com.ibm.icu" % "icu4j" % "76.1"
libraryDependencies += "org.apache.commons" % "commons-csv" % "1.13.0"
libraryDependencies += "com.chuusai" %% "shapeless" % "2.3.12"

publishTo := Some("GitHub Maven package repo for GSS-Cogs" at "https://maven.pkg.github.com/gss-cogs/csvw-check")
publishMavenStyle := true
credentials += Credentials(
  "GitHub Package Registry",
  "maven.pkg.github.com",
  "gss-cogs",
  sys.env.getOrElse("GITHUB_TOKEN", "")
)

organizationName := "Crown Copyright (Office for National Statistics)"
startYear := Some(2020)
licenses += ("Apache-2.0", new URI("https://www.apache.org/licenses/LICENSE-2.0.txt").toURL)
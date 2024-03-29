name := "csvw-check"

organization := "ONS"
version := "0.0.1"
maintainer := "csvcubed@gsscogs.uk"

scalaVersion := "2.13.4"
scalacOptions ++= Seq("-deprecation", "-feature")
autoCompilerPlugins := true

enablePlugins(JavaAppPackaging)
enablePlugins(DockerPlugin)
enablePlugins(UniversalPlugin)

dockerBaseImage := "openjdk:11"
dockerEntrypoint := Seq("bash")
dockerEnvVars := Map("PATH" -> "$PATH:/opt/docker/bin")
Docker / packageName := "csvwcheck"

libraryDependencies += "io.cucumber" %% "cucumber-scala" % "8.14.1" % Test
libraryDependencies += "io.cucumber" % "cucumber-junit" % "7.11.1" % Test
libraryDependencies += "com.novocode" % "junit-interface" % "0.11" % Test
libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.15" % Test
libraryDependencies += "com.github.pathikrit" %% "better-files" % "3.9.2" % Test

libraryDependencies += "org.scala-lang.modules" %% "scala-parser-combinators" % "1.1.2"
libraryDependencies += "io.spray" %% "spray-json" % "1.3.6"
libraryDependencies += "org.apache.jena" % "jena-arq" % "4.4.0"
libraryDependencies += "joda-time" % "joda-time" % "2.12.2"
libraryDependencies += "com.github.scopt" %% "scopt" % "4.1.0"
libraryDependencies += "com.typesafe.akka" %% "akka-stream" % "2.8.0"
libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.4.6"
libraryDependencies += "com.typesafe.scala-logging" %% "scala-logging" % "3.9.5"
libraryDependencies += "com.fasterxml.jackson.core" % "jackson-databind" % "2.14.2"
libraryDependencies += "com.fasterxml.jackson.core" % "jackson-annotations" % "2.14.2"
libraryDependencies += "com.fasterxml.jackson.core" % "jackson-core" % "2.14.2"
libraryDependencies += "com.softwaremill.sttp.client3" %% "core" % "3.8.15"
libraryDependencies += "com.ibm.icu" % "icu4j" % "72.1"
libraryDependencies += "org.apache.commons" % "commons-csv" % "1.10.0"
libraryDependencies += "com.chuusai" %% "shapeless" % "2.3.10"
// Here, `libraryDependencies` is a set of dependencies, and by using `+=`,
// we're adding the scala-parser-combinators dependency to the set of dependencies
// that sbt will go and fetch when it starts up.
// Now, in any Scala file, you can import classes, objects, etc., from
// scala-parser-combinators with a regular import.

// TIP: To find the "dependency" that you need to add to the
// `libraryDependencies` set, which in the above example looks like this:

// "org.scala-lang.modules" %% "scala-parser-combinators" % "1.1.2"

// You can use Scaladex, an index of all known published Scala libraries. There,
// after you find the library you want, you can just copy/paste the dependency
// information that you need into your build file. For example, on the
// scala/scala-parser-combinators Scaladex page,
// https://index.scala-lang.org/scala/scala-parser-combinators, you can copy/paste
// the sbt dependency from the sbt box on the right-hand side of the screen.

// IMPORTANT NOTE: while build files look _kind of_ like regular Scala, it's
// important to note that syntax in *.sbt files doesn't always behave like
// regular Scala. For example, notice in this build file that it's not required
// to put our settings into an enclosing object or class. Always remember that
// sbt is a bit different, semantically, than vanilla Scala.

// ============================================================================

// Most moderately interesting Scala projects don't make use of the very simple
// build file style (called "bare style") used in this build.sbt file. Most
// intermediate Scala projects make use of so-called "multi-project" builds. A
// multi-project build makes it possible to have different folders which sbt can
// be configured differently for. That is, you may wish to have different
// dependencies or different testing frameworks defined for different parts of
// your codebase. Multi-project builds make this possible.

// Here's a quick glimpse of what a multi-project build looks like for this
// build, with only one "subproject" defined, called `root`:

// lazy val root = (project in file(".")).
//   settings(
//     inThisBuild(List(
//       organization := "ch.epfl.scala",
//       scalaVersion := "2.13.1"
//     )),
//     name := "hello-world"
//   )

// To learn more about multi-project builds, head over to the official sbt
// documentation at http://www.scala-sbt.org/documentation.html
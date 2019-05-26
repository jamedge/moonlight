import sbtassembly.AssemblyKeys.assemblyMergeStrategy
import sbtassembly.MergeStrategy

ThisBuild / scalaVersion := "2.12.8"
ThisBuild / organization := "jamedge"

name := "moonlight"

val commonSettings = Seq(
  scalaVersion := "2.12.8",

  javacOptions ++= Seq("-source", "1.8", "-target", "1.8"),
  javaOptions ++= Seq("-Xms1024M", "-Xmx2200M"),

  Test / fork := true,
  Test / javaOptions ++= Seq("-Dconfig.resource=application.test.conf"),
  Test / testOptions += Tests.Argument(TestFrameworks.ScalaTest, "-oF"),
  Test / parallelExecution := false,

  libraryDependencies ++= Seq(
    "org.scalatest" %% "scalatest" % "3.0.5" % "test",
    "org.mockito" % "mockito-all" % "1.9.5" % "test",
    "com.github.tomakehurst" % "wiremock-standalone" % "2.19.0" % "test",
    "com.typesafe" % "config" % "1.2.1",
    "org.scalaj" %% "scalaj-http" % "2.4.1",
    "com.github.pureconfig" %% "pureconfig" % "0.7.2",
    "com.softwaremill.macwire" %% "macros" % "2.3.0",
    "com.softwaremill.macwire" %% "util" % "2.3.0",
    "com.softwaremill.macwire" %% "proxy" % "2.3.0",
    "org.slf4j" % "slf4j-simple" % "1.7.7",
  )
)

lazy val core = project.in(file("moonlight-core")).
  settings(commonSettings).
  settings(
    name := "moonlight-core",
    version := "0.0.1-SNAPSHOT",
    organization := "jamedge",

    updateOptions := updateOptions.value.withCachedResolution(true),

    resolvers ++= Seq(
      "Java.net Maven2 Repository" at "http://download.java.net/maven/2/",
      "Akka Repository" at "http://repo.akka.io/releases/",
      DefaultMavenRepository
    ),

    assemblyMergeStrategy in assembly := {
      case x if x startsWith "javax/xml" => MergeStrategy.last
      case x if x startsWith "org/apache/commons" => MergeStrategy.last
      case x if x endsWith "io.netty.versions.properties" => MergeStrategy.last
      case x =>
        val oldStrategy = (assemblyMergeStrategy in assembly).value
        oldStrategy(x)
    },

    publishTo := Some(Resolver.file("file", new File(Path.userHome.absolutePath+"/.m2/repository"))), // TODO: publish to external repo
    //credentials += Credentials(new File("credentials.properties"))
  ).
  settings(libraryDependencies ++= Seq(
    "com.typesafe.akka" %% "akka-http-core" % "10.0.11",
    "com.typesafe.akka" %% "akka-http" % "10.0.11",
    "com.typesafe.akka" %% "akka-http-testkit" % "10.0.11"
  ))

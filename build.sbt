import sbtassembly.AssemblyKeys.assemblyMergeStrategy
import sbtassembly.MergeStrategy

ThisBuild / scalaVersion := "2.13.1"
ThisBuild / organization := "com.github.jamedge"
ThisBuild / scalacOptions ++= Seq("-feature", "-deprecation")

name := "moonlight"

val commonSettings = Seq(
  javacOptions ++= Seq("-source", "1.8", "-target", "1.8"),
  javaOptions ++= Seq("-Xms1024M", "-Xmx2200M", "-Xss8M"),
  
  Test / fork := true,
  Test / javaOptions ++= Seq("-Dconfig.resource=application.test.conf"),
  Test / testOptions += Tests.Argument(TestFrameworks.ScalaTest, "-oF"),
  Test / parallelExecution := false,

  libraryDependencies ++= Seq(
    "org.scalatest" %% "scalatest" % "3.1.0" % "test",
    "org.mockito" % "mockito-all" % "1.9.5" % "test",
    "com.github.tomakehurst" % "wiremock-standalone" % "2.19.0" % "test",

    "com.softwaremill.macwire" %% "macros" % "2.3.3",
    "com.softwaremill.macwire" %% "util" % "2.3.3",
    "com.softwaremill.macwire" %% "proxy" % "2.3.3",

    "com.typesafe" % "config" % "1.4.0",
    "com.github.pureconfig" %% "pureconfig" % "0.12.2",

    "org.scalaj" %% "scalaj-http" % "2.4.2",
    "org.slf4j" % "slf4j-log4j12" % "1.7.7",
    "org.json4s" %% "json4s-jackson" % "3.7.0-M1"
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
    "com.typesafe.akka" %% "akka-stream" % "2.6.1",
    "com.typesafe.akka" %% "akka-http-core" % "10.1.11",
    "com.typesafe.akka" %% "akka-http" % "10.1.11",
    "com.typesafe.akka" %% "akka-http-testkit" % "10.1.11",

    "org.neo4j.driver" % "neo4j-java-driver" % "1.7.5",
    "com.dimafeng" %% "neotypes" % "0.13.0",
    "org.scala-graph" %% "graph-core" % "1.13.2",
    "org.scala-graph" %% "graph-json" % "1.13.0"
  ))

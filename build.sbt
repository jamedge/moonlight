import sbt.Keys.mainClass
import sbtassembly.AssemblyKeys.assemblyMergeStrategy
import sbtassembly.{MergeStrategy, PathList}

ThisBuild / scalaVersion := "2.13.1"
ThisBuild / organization := "com.github.jamedge"

ThisBuild / scalacOptions ++= Seq("-feature", "-deprecation")

val githubRepoOwner = "jamedge"
val githubRepoName = "moonlight"
val githubRepoUrl = url(s"https://github.com/$githubRepoOwner/$githubRepoName")
val githubRepoPackageUrlString = s"https://maven.pkg.github.com/$githubRepoOwner/$githubRepoName"

name := githubRepoName

lazy val commonSettings = Seq(
  resolvers ++= Seq(
    "Akka Repository" at "https://repo.akka.io/releases/",
    DefaultMavenRepository,
    Resolver.jcenterRepo
  ),

  credentials += Credentials(
    "GitHub Package Registry",
    "maven.pkg.github.com",
    githubRepoOwner,
    sys.env.getOrElse("GITHUB_TOKEN", "N/A")),

  javacOptions ++= Seq("-source", "1.8", "-target", "1.8"),
  javaOptions ++= Seq("-Xms1024M", "-Xmx2200M", "-Xss8M"),
  
  Test / fork := true,
  Test / javaOptions ++= Seq("-Dconfig.resource=application.test.conf"),
  Test / testOptions += Tests.Argument(TestFrameworks.ScalaTest, "-oF"),
  Test / parallelExecution := false,

  libraryDependencies ++= Seq(
    "org.scalatest" %% "scalatest" % "3.1.1" % "test",
    "org.scalatestplus" %% "scalatestplus-mockito" % "1.0.0-M2",
    "org.mockito" % "mockito-core" % "3.3.3" % "test",
    "org.pegdown" % "pegdown" % "1.4.2" % "test",
    "com.github.tomakehurst" % "wiremock-standalone" % "2.19.0" % "test",

    "com.softwaremill.macwire" %% "macros" % "2.3.3",
    "com.softwaremill.macwire" %% "util" % "2.3.3",
    "com.softwaremill.macwire" %% "proxy" % "2.3.3",

    "com.typesafe" % "config" % "1.4.0",
    "com.github.pureconfig" %% "pureconfig" % "0.12.2",

    "org.slf4j" % "slf4j-log4j12" % "1.7.7"
  )
)

lazy val assemblySettings = Seq(
  updateOptions := updateOptions.value.withCachedResolution(true),

  assemblyMergeStrategy in assembly := {
    case "reference.conf" => MergeStrategy.concat
    case "application.conf" => MergeStrategy.concat
    case PathList("META-INF", "services", _*) => MergeStrategy.concat
    case PathList("META-INF", "log4j-provider.properties") => MergeStrategy.concat
    case x if x startsWith "META-INF" => MergeStrategy.discard
    case _ => MergeStrategy.last
  },

  assemblyJarName in assembly := s"${name.value}.jar", // just if jar needs to be used separately for testing
)

lazy val apiSettings = Seq(
  libraryDependencies ++= Seq(
    "com.typesafe.akka" %% "akka-stream" % "2.6.4",
    "com.typesafe.akka" %% "akka-stream-testkit" % "2.6.4",
    "com.typesafe.akka" %% "akka-http-core" % "10.1.12",
    "com.typesafe.akka" %% "akka-http" % "10.1.12",
    "com.typesafe.akka" %% "akka-http-testkit" % "10.1.12",
    "ch.megard" %% "akka-http-cors" % "0.4.1",

    "org.scalaj" %% "scalaj-http" % "2.4.2",
    "org.json4s" %% "json4s-jackson" % "3.7.0-M1"
  )
)

lazy val publishSettings = Seq(
  publishTo := Some("GitHub Package Registry" at githubRepoPackageUrlString),
  resolvers ++= Seq(s"GitHub Package Registry ($githubRepoOwner/$githubRepoName)" at githubRepoPackageUrlString),
  scmInfo := Some(ScmInfo(githubRepoUrl, s"scm:git@github.com:$githubRepoOwner/$githubRepoName.git")),
  homepage := Some(githubRepoUrl),
  pomIncludeRepository := (_ => false),
  publishMavenStyle := true
)

def dockerSettings(exposePort: Option[Int] = None) = Seq(
  dockerfile in docker := {
    val artifact: File = assembly.value
    val artifactTargetPath = s"/usr/local/lib/app.jar"
    val startScriptLocalPath = new File("./bin/start.sh")
    val startScriptTargetPath = "/usr/local/lib/start.sh"
    val startScript = new File(startScriptTargetPath)
    new Dockerfile {
      from("java:openjdk-8")
      env("ARTIFACT_PATH", artifactTargetPath)
      add(artifact, artifactTargetPath)
      add(startScriptLocalPath, startScript)
      entryPoint(startScriptTargetPath)
      if (exposePort.isDefined) {
        expose(exposePort.get)
      }
    }
  },

  imageNames in docker := {
    val dockerhubRegistry = "markojamedzija"
    val dockerhubRepository = name.value
    val finalImageName = dockerhubRegistry + "/" + dockerhubRepository
    val latestTag = "latest"
    val gitCommitTag = git.gitHeadCommit.value map { sha => sha.take(7) }
    Seq(
      ImageName(s"$finalImageName:$latestTag"), // TODO: add version tag when it's added
      ImageName(
        registry = Some(dockerhubRegistry),
        namespace = None,
        repository = dockerhubRepository,
        tag = gitCommitTag
      )
    )
  }
)

lazy val `moonlight-core` = project.in(file("moonlight-core")).
  enablePlugins(DockerPlugin).
  settings(commonSettings).
  settings(libraryDependencies ++= Seq(
    "org.neo4j.driver" % "neo4j-java-driver" % "1.7.5",
    "com.dimafeng" %% "neotypes" % "0.13.0",
    "org.scala-graph" %% "graph-core" % "1.13.2",
    "org.scala-graph" %% "graph-json" % "1.13.0"
  ))

lazy val `moonlight-api` = project.in(file("moonlight-api")).
  dependsOn(`moonlight-core` % "test->test;compile->compile").
  enablePlugins(DockerPlugin).
  settings(commonSettings).
  settings(assemblySettings).
  settings(dockerSettings(Some(8080))).
  settings(apiSettings).
  settings(
    mainClass in assembly := Some("com.github.jamedge.moonlight.api.Api")
  ).
  settings(libraryDependencies ++= Seq(
    "org.scala-graph" %% "graph-core" % "1.13.2",
    "org.scala-graph" %% "graph-json" % "1.13.0",
    "com.vladsch.flexmark" % "flexmark-all" % "0.62.2"
  ))

lazy val `moonlight-client` = project.in(file("moonlight-client")).
  dependsOn(`moonlight-core` % "test->test;compile->compile").
  settings(commonSettings).
  settings(publishSettings).
  settings(
    version := "0.0.4"
  )

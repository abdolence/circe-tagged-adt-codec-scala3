import java.time.format.DateTimeFormatter
import java.time.{ ZoneOffset, ZonedDateTime }
import sbt.Package.ManifestAttributes
import sbtcrossproject.CrossType
import sbtcrossproject.CrossPlugin.autoImport.crossProject

name := "circe-tagged-adt-codec-root"

ThisBuild / version := "0.10.0-SNAPSHOT"

ThisBuild / versionScheme := Some("semver-spec")

ThisBuild / organization := "org.latestbit"

ThisBuild / homepage := Some( url( "https://github.com/abdolence/circe-tagged-adt-codec" ) )

ThisBuild / licenses := Seq(
  (
    "Apache License v2.0",
    url( "http://www.apache.org/licenses/LICENSE-2.0.html" )
  )
)

ThisBuild / crossScalaVersions := Seq( "3.0.0" )

ThisBuild / scalaVersion := (ThisBuild / crossScalaVersions).value.head

ThisBuild / exportJars := true

ThisBuild / publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (isSnapshot.value)
    Some( "snapshots" at nexus + "content/repositories/snapshots" )
  else
    Some( "releases" at nexus + "service/local/staging/deploy/maven2" )
}

ThisBuild / pomExtra := (
  <scm>
	<url>https://github.com/abdolence/circe-tagged-adt-codec</url>
	<connection>scm:git:https://github.com/abdolence/circe-tagged-adt-codec</connection>
	<developerConnection>scm:git:https://github.com/abdolence/circe-tagged-adt-codec</developerConnection>
  </scm>
  <developers>
	<developer>
	<id>abdulla</id>
	<name>Abdulla Abdurakhmanov</name>
	<url>https://abdolence.dev</url>
	</developer>
  </developers>
)

ThisBuild / resolvers ++= Seq(
  "Typesafe repository" at "https://repo.typesafe.com/typesafe/releases/",
  "Sonatype OSS Releases" at "https://oss.sonatype.org/content/repositories/releases/"
)

def baseScalacOptions(scalaVersionStr: String) = Seq(
  "-deprecation",
  "-unchecked",
  "-feature",
  "-language:higherKinds",
) ++ (CrossVersion.partialVersion( scalaVersionStr ) match {
  case Some( ( 2, n ) ) if n >= 13 => Seq( "-Xsource:3" )
  case Some( ( 2, n ) ) if n < 13  => Seq( "-Ypartial-unification" )
  case _                           => Seq()
})

ThisBuild / javacOptions ++= Seq(
  "-Xlint:deprecation",
  "-source",
  "1.8",
  "-target",
  "1.8",
  "-Xlint"
)

val scalacJsOptions = Seq()

ThisBuild / packageOptions := Seq(
  ManifestAttributes(
    ( "Build-Jdk", System.getProperty( "java.version" ) ),
    (
      "Build-Date",
      ZonedDateTime
        .now( ZoneOffset.UTC )
        .format( DateTimeFormatter.ISO_OFFSET_DATE_TIME )
    )
  )
)

val circeVersion = "0.14.0"
val scalaTestVersion = "3.2.9"

val baseJvmDependencies =
  Seq(
    "io.circe" %% "circe-core",
    "io.circe" %% "circe-generic",
    "io.circe" %% "circe-parser"
  ).map( _ % circeVersion ) ++
    Seq(
      "org.scalactic" %% "scalactic",
      "org.scalatest" %% "scalatest"
    ).map( _ % scalaTestVersion % Test )

val baseJsDependencies =
  Def.setting(
    Seq(
      "io.circe" %%% "circe-core",
      "io.circe" %%% "circe-generic",
      "io.circe" %%% "circe-parser"
    ).map( _ % circeVersion ) ++
      Seq(
        "org.scalactic" %%% "scalactic",
        "org.scalatest" %%% "scalatest"
      ).map( _ % scalaTestVersion % Test )
  )

lazy val circeTaggedAdtCodecRoot =
  (project in file( "." ))
    .aggregate( circeTaggedAdtCodecLib )
    .settings(
      publish := {},
      publishLocal := {},
      crossScalaVersions := List(),
      scalacOptions := baseScalacOptions(scalaVersion.value)
    )

lazy val circeTaggedAdtCodecLib =
  project
    .in( file( "lib" ) )
    .aggregate( circeTaggedAdtCodecLibJVM, circeTaggedAdtCodecLibJS )
    .settings(
      name := "circe-tagged-adt-codec",
      publish := {},
      publishLocal := {},
      crossScalaVersions := List(),
      scalacOptions := baseScalacOptions(scalaVersion.value)
    )

lazy val circeTaggedAdtCodecLibCross = crossProject( JSPlatform, JVMPlatform )
  .withoutSuffixFor( JVMPlatform )
  .crossType( CrossType.Full )
  .in( file( "lib" ) )
  .settings(
    name := "circe-tagged-adt-codec",
    scalacOptions := baseScalacOptions(scalaVersion.value)
  )
  .jvmSettings(
    libraryDependencies ++= baseJvmDependencies ++ Seq()
  )
  .jsSettings(
    libraryDependencies ++= baseJsDependencies.value ++ Seq(),
    scalacOptions ++= scalacJsOptions,
    test := false
  )

lazy val circeTaggedAdtCodecLibJVM = circeTaggedAdtCodecLibCross.jvm
lazy val circeTaggedAdtCodecLibJS = circeTaggedAdtCodecLibCross.js


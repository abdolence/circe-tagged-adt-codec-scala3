import java.time.format.DateTimeFormatter
import java.time.{ZoneOffset, ZonedDateTime}

import sbt.Package.ManifestAttributes

name := "slack-morphism-root"

ThisBuild / version := "1.0.0-SNAPSHOT"

ThisBuild / organization := "org.latestbit"

ThisBuild / homepage := Some(url("https://latestbit.com"))

ThisBuild / licenses := Seq(("Apache License v2.0", url("http://www.apache.org/licenses/LICENSE-2.0.html")))

ThisBuild / scalaVersion := "2.12.10"

ThisBuild / crossScalaVersions := Seq("2.12.10")

ThisBuild / sbtVersion := "1.3.8"

ThisBuild / scalacOptions ++= Seq("-feature")

ThisBuild / exportJars := true

publishArtifact := false

publishTo := Some(Resolver.file("Unused transient repository", file("target/unusedrepo")))

ThisBuild / resolvers ++= Seq(
	"Typesafe repository" at "https://repo.typesafe.com/typesafe/releases/",
	"Sonatype OSS Releases" at "https://oss.sonatype.org/content/repositories/releases/",
	"Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/"
)

ThisBuild / scalacOptions ++= Seq(
	"-deprecation",
	"-unchecked",
	"-feature",
	"-Xsource:2.12",
	"-Ypartial-unification",
	"-language:higherKinds"
)

ThisBuild / javacOptions ++= Seq("-Xlint:deprecation", "-source", "1.8", "-target", "1.8", "-Xlint")

ThisBuild / packageOptions := Seq(ManifestAttributes(
	("Build-Jdk", System.getProperty("java.version")),
	("Build-Date",  ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME) )
))

val circeVersion = "0.12.3"
val scalaTestVersion = "3.1.0"
val sttpVer = "2.0.0-RC5"

val baseDependencies =
	Seq (
		"io.circe" %% "circe-core",
		"io.circe" %% "circe-generic",
		"io.circe" %% "circe-parser"
	).
		map(_ % circeVersion) ++
	Seq(
		"io.circe" %% "circe-generic-extras" % "0.12.2"
	) ++
	Seq(
		"org.scalactic" %% "scalactic",
		"org.scalatest" %% "scalatest"
	).
		map(_ % scalaTestVersion % "test")

addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.1" cross CrossVersion.full)

lazy val slackMorphismRoot = project.in(file(".")).
	aggregate(slackMorphismModels,slackMorphismClient, slackMorphismExamples).
	settings(
		publish := {},
		publishLocal := {},
		crossScalaVersions := List()
	)

lazy val slackMorphismModels =
	(project in file("models")).
		settings(
			name := "slack-morphism-models",
			libraryDependencies ++= baseDependencies ++ Seq(
			)
		)

lazy val slackMorphismClient =
	(project in file("client")).
		settings(
			name := "slack-morphism-client",
			libraryDependencies ++= baseDependencies ++ Seq(
				"com.softwaremill.sttp.client" %% "core" % sttpVer
			)
		).
		dependsOn(slackMorphismModels)

lazy val slackMorphismExamples =
	(project in file("examples")).
		settings(
			name := "slack-morphism-client",
			libraryDependencies ++= baseDependencies ++ Seq(
			)
		).
		dependsOn(slackMorphismClient)
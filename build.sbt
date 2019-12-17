import java.time.format.DateTimeFormatter
import java.time.{ZoneOffset, ZonedDateTime}

import sbt.Package.ManifestAttributes

name := "circe-tagged-adt-codec-root"

ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / organization := "org.latestbit"

ThisBuild / homepage := Some(url("https://latestbit.com"))

ThisBuild / licenses := Seq(("Apache License v2.0", url("http://www.apache.org/licenses/LICENSE-2.0.html")))

ThisBuild / scalaVersion := "2.12.10"

ThisBuild / crossScalaVersions := Seq("2.12.10", "2.13.1")

ThisBuild / sbtVersion := "1.3.5"

ThisBuild / scalacOptions ++= Seq("-feature")

ThisBuild / exportJars := true

publishMavenStyle := true

ThisBuild / publishTo := {
	val nexus = "https://oss.sonatype.org/"
	if (isSnapshot.value)
		Some("snapshots" at nexus + "content/repositories/snapshots")
	else
		Some("releases"  at nexus + "service/local/staging/deploy/maven2")
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
				<url>https://latestbit.com</url>
			</developer>
		</developers>
	)


ThisBuild / resolvers ++= Seq(
	"Typesafe repository" at "https://repo.typesafe.com/typesafe/releases/",
	"Sonatype OSS Releases" at "https://oss.sonatype.org/content/repositories/releases/"
)

ThisBuild / scalacOptions ++= Seq(
	"-deprecation",
	"-unchecked",
	"-feature",
	"-Xsource:2.12",
	"-language:higherKinds"
)

ThisBuild / javacOptions ++= Seq("-Xlint:deprecation", "-source", "1.8", "-target", "1.8", "-Xlint")

ThisBuild / packageOptions := Seq(ManifestAttributes(
	("Build-Jdk", System.getProperty("java.version")),
	("Build-Date",  ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME) )
))

val circeVersion = "0.12.3"
val scalaTestVersion = "3.1.0"

val baseDependencies =
	Seq (
		"io.circe" %% "circe-core",
		"io.circe" %% "circe-generic",
		"io.circe" %% "circe-parser"
	).
		map(_ % circeVersion) ++
	Seq(
		"org.scalactic" %% "scalactic",
		"org.scalatest" %% "scalatest"
	).
		map(_ % scalaTestVersion % "test")

lazy val circeTaggedAdtCodecRoot =
	(project in file(".")).
	aggregate(circeTaggedAdtCodecMacros,circeTaggedAdtCodecLib).
	settings(
		publish := {},
		publishLocal := {},
		crossScalaVersions := List()
	)

lazy val circeTaggedAdtCodecMacros =
	(project in file("macros")).
		settings(
			name := "circe-tagged-adt-codec-macros",
			libraryDependencies ++= baseDependencies ++ Seq(
				"org.scala-lang" % "scala-reflect" % scalaVersion.value
			)
		)
lazy val circeTaggedAdtCodecLib =
	(project in file("lib")).
		settings(
			name := "circe-tagged-adt-codec",
			libraryDependencies ++= baseDependencies ++ Seq(
			)
		).
		dependsOn(circeTaggedAdtCodecMacros)

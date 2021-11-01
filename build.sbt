Global / onChangedBuildSource := ReloadOnSourceChanges

ThisBuild / versionScheme := Some("early-semver")

sbtPlugin		:= true

name			:= "xsbt-kapsel"
organization	:= "de.djini"
version			:= "0.5.0"

scalacOptions	++= Seq(
	"-feature",
	"-deprecation",
	"-unchecked",
	"-Xfatal-warnings",
)

conflictManager	:= ConflictManager.strict withOrganization "^(?!(org\\.scala-lang|org\\.scala-js|org\\.scala-sbt)(\\..*)?)$"

addSbtPlugin("de.djini" % "xsbt-util"		% "1.6.0")
addSbtPlugin("de.djini" % "xsbt-classpath"	% "2.8.0")

libraryDependencies	+= "de.djini" %	"kapsel-start" % "0.3.0"

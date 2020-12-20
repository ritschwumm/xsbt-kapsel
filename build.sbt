Global / onChangedBuildSource := ReloadOnSourceChanges

sbtPlugin		:= true

name			:= "xsbt-kapsel"
organization	:= "de.djini"
version			:= "0.2.0"

scalacOptions	++= Seq(
	"-feature",
	"-deprecation",
	"-unchecked",
	"-Xfatal-warnings",
)

conflictManager	:= ConflictManager.strict withOrganization "^(?!(org\\.scala-lang|org\\.scala-js|org\\.scala-sbt)(\\..*)?)$"

addSbtPlugin("de.djini" % "xsbt-util"		% "1.5.0")
addSbtPlugin("de.djini" % "xsbt-classpath"	% "2.7.0")

libraryDependencies	+= "de.djini" %	"kapsel-start" % "0.1.0"

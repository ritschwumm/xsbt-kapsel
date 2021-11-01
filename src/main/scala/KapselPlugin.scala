package xsbtKapsel

import java.util.jar.Attributes.Name._

import sbt._
import sbt.io.Using
import Keys.TaskStreams

import xsbtUtil.implicits._
import xsbtUtil.types._
import xsbtUtil.{ util => xu }

import xsbtClasspath.{ Asset => ClasspathAsset, ClasspathPlugin }
import xsbtClasspath.Import.classpathAssets

object Import {
	val kapselBuildDir			= settingKey[File]("base directory of built files")
	val kapselBundleId			= settingKey[String]("name of the package built")

	val kapsel					= taskKey[File]("complete build, returns the created kapsel jar")
	val kapselJarFile			= taskKey[File]("the kapsel jar file")
	val kapselMakeExecutable	= settingKey[Boolean]("make the jar file executable on unixoid systems")

	val kapselMainClass			= taskKey[Option[String]]("name of the main class")
	val kapselVmOptions			= settingKey[Seq[String]]("vm options like -Xmx128")
	val kapselSystemProperties	= settingKey[Map[String,String]]("-D in the command line")
}

object KapselPlugin extends AutoPlugin {
	//------------------------------------------------------------------------------
	//## constants

	private val kapselClassName		= "Kapsel"
	private val kapselClassFileName	= kapselClassName + ".class"
	private val kapselClassResource	= "/" + kapselClassFileName
	private val execHeaderResource	= "/exec-header.sh"

	//------------------------------------------------------------------------------
	//## exports

	override val requires:Plugins		= ClasspathPlugin && plugins.JvmPlugin

	override val trigger:PluginTrigger	= noTrigger

	lazy val autoImport	= Import
	import autoImport._

	override lazy val projectSettings:Seq[Def.Setting[_]]	=
		Vector(
			kapsel			:=
				buildTask(
					streams				= Keys.streams.value,
					assets				= classpathAssets.value,
					jarFile				= kapselJarFile.value,
					makeExecutable		= kapselMakeExecutable.value,
					bundleId			= kapselBundleId.value,
					vmOptions			= kapselVmOptions.value,
					systemProperties	= kapselSystemProperties.value,
					mainClass			= kapselMainClass.value,
				),
			kapselBuildDir			:= Keys.crossTarget.value / "kapsel",
			kapselJarFile			:= kapselBuildDir.value / (kapselBundleId.value + ".jar"),
			kapselMakeExecutable	:= false,

			kapselBundleId			:= Keys.name.value + "-" + Keys.version.value,
			kapselMainClass			:= (Runtime / Keys.mainClass).value,
			kapselVmOptions			:= Seq.empty,
			kapselSystemProperties	:= Map.empty,
		)

	//------------------------------------------------------------------------------
	//## tasks

	private def buildTask(
		streams:TaskStreams,
		assets:Seq[ClasspathAsset],
		jarFile:File,
		makeExecutable:Boolean,

		bundleId:String,
		vmOptions:Seq[String],
		systemProperties:Map[String,String],
		mainClass:Option[String],
	):File =
		IO withTemporaryDirectory { tempDir =>
			val mainClassGot:String	=
				mainClass getOrElse {
					xu.fail logging (streams, s"${kapselMainClass.key.label} must be set")
				}

			val kapselClassFile	= tempDir / kapselClassFileName
			IO write (kapselClassFile, xu.classpath bytes kapselClassResource)
			//Using.urlInputStream(xu.classpath url kapselClassResource)(IO.transfer(_, kapselClassFile))
			val kapselSource	= kapselClassFile -> kapselClassFileName

			val systemPropertyOptions	= xu.script systemProperties systemProperties

			val classPath	= assets map (_.name)
			val manifest	=
				xu.jar manifest (
					MANIFEST_VERSION.toString	-> "1.0",
					MAIN_CLASS.toString			-> kapselClassName,
					"Kapsel-Application-Id"		-> bundleId,
					"Kapsel-Jvm-Options"		-> (vmOptions ++ systemPropertyOptions).mkString(" "),
					"Kapsel-Main-Class"			-> mainClassGot,
					"Kapsel-Class-Path"			-> classPath.mkString(" "),
				)

			streams.log info s"building kapsel file ${jarFile}"
			jarFile.mkParentDirs()

			val assetSources	= assets map (_.flatPathMapping)
			val jarSources		= kapselSource +: assetSources
			// TODO should we use fixed timestamps?
			if (makeExecutable) {
				val tempJar	= tempDir / "kapsel.jar"
				IO jar		(jarSources, tempJar, manifest, None)

				IO write	(jarFile, xu.classpath bytes execHeaderResource)
				IO append	(jarFile, IO readBytes tempJar)

				jarFile.setExecutable(true, false)
			}
			else {
				IO jar (jarSources, jarFile, manifest, None)
			}

			jarFile
		}
}

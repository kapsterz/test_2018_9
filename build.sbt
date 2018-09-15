import Settings._
import Dependencies._

name := "test"
 
version := "1.0"

lazy val root = (project in file(".")).aggregate(slaServiceApi)

lazy val slaServiceApi = (project in file("slaServiceApi"))
  .settings(name := "test")
  .enablePlugins(PlayScala)
  .settings(Settings.commonSettings, commonBuildSettings, webBuildSettings)
  .settings(Core.deps, Testing.deps)





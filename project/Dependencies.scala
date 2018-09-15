import play.sbt.PlayImport._
import sbt.Keys.libraryDependencies
import sbt.{Def, ModuleID, _}
import sbt.Keys._

object Dependencies {
  object Version {
    val `scala-logging`    = "3.7.2"
    val scalamock = "3.6.0"
    val scalatest = "3.0.5"
    val scalaTestPlusPlay = "3.1.2"
  }

  object Core {
    val `scala-logging` = "com.typesafe.scala-logging" %% "scala-logging" % Version.`scala-logging`
    val deps = Seq(
      libraryDependencies ++= compile(`scala-logging`, jdbc , ehcache , ws, guice))
  }

  object Testing {
    val scalatest = "org.scalatest" %% "scalatest" % Version.scalatest
    val scalatestplusPlay = "org.scalatestplus.play" %% "scalatestplus-play" % Version.scalaTestPlusPlay
    val scalamock = "org.scalamock" %% "scalamock-scalatest-support" % Version.scalamock

    val deps = Seq(libraryDependencies ++= test(scalatest, scalatestplusPlay, scalamock))
  }

  def test(m: ModuleID*): Seq[ModuleID] = m map (_ % "test")
  def compile(m: ModuleID*): Seq[ModuleID] = m map (_ % "compile")

}

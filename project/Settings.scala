import com.typesafe.sbt.packager.Keys._
import com.typesafe.sbt.packager.universal.UniversalPlugin.autoImport._
import play.sbt.routes.RoutesKeys.{InjectedRoutesGenerator, routesGenerator}
import sbt.Keys._
import sbt._

object Settings {

  lazy val commonSettings = Seq(
    scalacOptions ++= Seq(
      "-deprecation", // warning and location for usages of deprecated APIs
      "-encoding",
      "UTF-8",
      "-feature", // warning and location for usages of features that should be imported explicitly
      "-language:postfixOps",
      "-language:implicitConversions",
      "-target:jvm-1.8",
      "-unchecked", // additional warnings where generated code depends on assumptions
      "-Ywarn-adapted-args", // Warn if an argument list is modified to match the receiver
      "-Ywarn-value-discard", // Warn when non-Unit expression results are unused
      "-Ywarn-inaccessible",
      "-Ywarn-dead-code",
      "-Xprint-types"
    )
  )

  val commonBuildSettings = Seq(
    organization in ThisBuild := "com.osmolovskyi.test",
    scalaVersion in ThisBuild := "2.12.2"
  )

  lazy val webBuildSettings = Seq(
    routesGenerator := InjectedRoutesGenerator,
    unmanagedResourceDirectories in Test += baseDirectory(_ / "target/slaServiceApi/public/test").value,
    javaOptions in Universal ++= Seq(
      "-Dfile.encoding=UTF-8",
      "-Dpidfile.path=/dev/null",
      "-Dorg.aspectj.tracing.factory=default"
    ),
    scriptClasspath ~= (cp => "custom/*" +: cp)
  )


}

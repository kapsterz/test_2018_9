package com.osmolovskyi.test.helpers

import javax.inject.{Inject, Singleton}
import play.api.Configuration

import scala.concurrent.duration._

@Singleton
class DefaultConfigs @Inject()(configuration: Configuration) {
  val userNameLength: Int = configuration.getOptional[Int]("slaService.user.nameLength").getOrElse(10)
  val rps: Int = configuration.getOptional[Int]("slaService.user.rps").getOrElse(10)
  val slaResponseTime: Int = configuration.getOptional[Int]("slaService.slaResponseTime").getOrElse(250)
  val userCount: Int = configuration.getOptional[Int]("slaService.user.userCount").getOrElse(1000)
  val graceRps: Int = configuration.getOptional[Int]("slaService.graceRps").getOrElse(30)
  val defaultTimeout: FiniteDuration =
    configuration
      .getOptional[Int]("slaService.defaultTimeout")
      .getOrElse(30)
      .seconds

  object LoadTest {

    val duration: FiniteDuration = configuration
      .getOptional[Int]("slaService.loadtest.duration")
      .getOrElse(30)
      .seconds

    val port: Int = configuration.getOptional[Int]("slaService.defaultPort").getOrElse(8118)

    val baseUrl: String =
      configuration.getOptional[String]("slaService.loadtest.baseUrlTest").getOrElse("http://localhost:9000/api/testSla")

    object QueueConfig {
      val bufferSize: Int = configuration.getOptional[Int]("slaService.loadtest.bufferSize").getOrElse(10000)
      val parallelism: Int = configuration.getOptional[Int]("slaService.loadtest.parallelism").getOrElse(100)
    }

  }

}

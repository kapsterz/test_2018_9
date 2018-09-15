package com.osmolovskyi.test.services

import com.osmolovskyi.test.helpers.DefaultValues
import com.osmolovskyi.test.models.Sla
import com.typesafe.scalalogging.LazyLogging
import javax.inject.{Inject, Singleton}
import play.api.Configuration

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Random

trait SlaService {
  def getSlaByToken(token: String): Future[Sla]
}

@Singleton
class SlaServiceImpl @Inject()(configuration: Configuration)
                              (implicit ec: ExecutionContext) extends SlaService with LazyLogging {
  private lazy val userPool: Seq[Sla] = (1 to userCount).map { _ =>
    val userName = Random.alphanumeric.take(userNameLength).mkString
    val rps = Random.nextInt(rpsRange)
    Sla(userName, rps)
  }

  private lazy val userNameLength: Int =
    configuration.getOptional[Int]("slaService.user.nameLength").getOrElse(DefaultValues.userNameLength)
  private lazy val rpsRange: Int =
    configuration.getOptional[Int]("slaService.user.rpsRange").getOrElse(DefaultValues.rpsRange)
  private lazy val slaResponseTime: Int =
    configuration.getOptional[Int]("slaService.user.rpsRange").getOrElse(DefaultValues.slaResponseTime)
  private lazy val userCount: Int =
    configuration.getOptional[Int]("slaService.userCount").getOrElse(DefaultValues.userCount)

  def getSlaByToken(token: String): Future[Sla] = Future {
    Thread.sleep(slaResponseTime)
    userPool(Random.nextInt(userCount))
  }

}



package com.osmolovskyi.test.services

import com.osmolovskyi.test.helpers._
import com.osmolovskyi.test.models.Sla
import com.typesafe.scalalogging.LazyLogging
import javax.inject.{Inject, Singleton}

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

trait SlaService {
  def getSlaByToken(token: String): Future[Sla]
}

@Singleton
class SlaServiceImpl @Inject()(defaultConfigs: DefaultConfigs,
                               slaHelper: SlaHelper)
                              (implicit ec: ExecutionContext) extends SlaService with LazyLogging {
  private lazy val userPool: Seq[Sla] =
    slaHelper.newUserPool(defaultConfigs.userCount, defaultConfigs.userNameLength, defaultConfigs.rps)

  def getSlaByToken(token: String): Future[Sla] = {
    FutureTask.schedule(defaultConfigs.slaResponseTime.millis) {
      slaHelper.nextUser(userPool, defaultConfigs.userCount, token)

    }.toFuture
  }

}



package com.osmolovskyi.test.actors

import akka.actor.{Actor, Props}
import com.osmolovskyi.test.actors.StatisticActor._
import com.osmolovskyi.test.models.StatisticReport
import com.typesafe.scalalogging.LazyLogging
import javax.inject.Singleton

@Singleton
class StatisticActor extends Actor with LazyLogging {
  def receiveWithStatistic(statisticReport: StatisticReport): Receive = {
    case statistic: Statistic =>
      context.become(receiveWithStatistic(statisticReport.updateStatistics(statistic)))
    case GetStatistic =>
      sender() ! statisticReport
      context.become(receiveWithStatistic(StatisticReport.empty))
    case message =>
      logger.error(s"Received unexpected message: $message")
  }

  override def receive: Receive = receiveWithStatistic(StatisticReport.empty)
}

object StatisticActor {

  def props(): Props = Props(new StatisticActor)

  case class Statistic(status: Int, isRequestAllowed: String, token: String)

  case object GetStatistic
}

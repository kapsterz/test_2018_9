package com.osmolovskyi.test.actors

import akka.actor.{Actor, PoisonPill, Props}
import com.osmolovskyi.test.actors.LoadTestServiceActor._
import com.osmolovskyi.test.services.LoadTestExecutionService
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

class LoadTestServiceActor(loadTestExecutionHelper: LoadTestExecutionService) extends Actor with LazyLogging {
  implicit val ec: ExecutionContext = context.system.dispatcher

  override def receive: Receive = {
    case BatchRequest =>
      (1 to 1000).map { _ =>
        loadTestExecutionHelper.executeQuery
      }
      context.system.scheduler.scheduleOnce(1.millis, self, BatchRequest)

    case RequestStatisticReport =>
      val sndr = sender()
      loadTestExecutionHelper.getStatistic.foreach { statistic =>
        sndr ! statistic
        self ! PoisonPill
      }
    case message =>
      logger.error(s"Received unexpected message: $message")
  }

  override def postStop(): Unit = logger.info("LoadTest Service Actor stopped")

}

object LoadTestServiceActor {

  def props(loadTestExecutionHelper: LoadTestExecutionService): Props =
    Props(new LoadTestServiceActor(loadTestExecutionHelper))

  case object BatchRequest

  case object RequestStatisticReport

}

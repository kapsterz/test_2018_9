package com.osmolovskyi.test.actors

import java.util.UUID

import akka.actor.{Actor, Props}
import com.osmolovskyi.test.actors.UserActor._
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

class UserActor(rps: Int)(implicit ec: ExecutionContext) extends Actor with LazyLogging {

  def receiveWithCounter(count: Int, rps: Int, skipProcessedIds: Set[UUID]): Receive = {
    case RequestInProgress(id) if count < rps =>
      context.become(receiveWithCounter(count + 1, rps, skipProcessedIds))
      context.system.scheduler.scheduleOnce(1.second, self, RequestProcessed(id))
      sender() ! true

    case RequestInProgress(_) =>
      context.system.scheduler.scheduleOnce(100.millis, self, LimitCounted((rps * 1.1).toInt))
      logger.warn(s"RPS limit exceeded. New limit will be applied after 0.1 second")
      sender() ! false

    case RequestProcessed(id) if skipProcessedIds.contains(id) =>
      context.become(receiveWithCounter(count, rps, skipProcessedIds - id))

    case RequestProcessed(_) =>
      context.become(receiveWithCounter(count, rps, skipProcessedIds))

    case LimitCounted(newRps) =>
      context.become(receiveWithCounter(count, newRps, skipProcessedIds))
      logger.info(s"RPS limit was changed to $newRps")

    case RequestForceProcessed(id) =>
      context.become(receiveWithCounter(count - 1, rps, skipProcessedIds + id))

    case message =>
      logger.error(s"Received unexpected message: $message")
  }

  override def receive: Receive = receiveWithCounter(0, rps, Set.empty[UUID])
}

object UserActor {

  def props(rps: Int)(implicit ec: ExecutionContext): Props = Props(new UserActor(rps)).withDispatcher("mailboxes.user-dispatcher")

  case class RequestInProgress(id: UUID)

  case class RequestProcessed(id: UUID)

  case class RequestForceProcessed(id: UUID)

  case class LimitCounted(newRps: Int)

}

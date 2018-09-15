package com.osmolovskyi.test.actors

import java.util.UUID

import akka.actor.{Actor, Props}
import com.osmolovskyi.test.actors.UserActor._

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

class UserActor(rps: Int)(implicit ec: ExecutionContext) extends Actor {

  def receiveWithCounter(count: Int, rps: Int, skipProcessedIds: Set[UUID]): Receive = {
    case RequestInProgress(id) if count < rps =>
      context.become(receiveWithCounter(count + 1, rps, skipProcessedIds))
      context.system.scheduler.scheduleOnce(1.second, self, RequestProcessed(id))
      sender() ! true

    case RequestInProgress(_) =>
      context.system.scheduler.scheduleOnce(100.millis, self, LimitCounted(rps))
      sender() ! false

    case RequestProcessed(id) if skipProcessedIds.contains(id) =>
      context.become(receiveWithCounter(count, rps, skipProcessedIds - id))

    case RequestProcessed(_) =>
      context.become(receiveWithCounter(count, rps, skipProcessedIds))

    case LimitCounted(newRps) =>
      context.become(receiveWithCounter(count, newRps, skipProcessedIds))

    case RequestForceProcessed(id) =>
      context.become(receiveWithCounter(count - 1, rps, skipProcessedIds + id))
  }

  override def receive: Receive = receiveWithCounter(0, rps, Set.empty[UUID])
}

object UserActor {

  def props(rps: Int)(implicit ec: ExecutionContext): Props = Props(new UserActor(rps))

  case class RequestInProgress(id: UUID)

  case class RequestProcessed(id: UUID)

  case class RequestForceProcessed(id: UUID)

  case class LimitCounted(newRps: Int)

}

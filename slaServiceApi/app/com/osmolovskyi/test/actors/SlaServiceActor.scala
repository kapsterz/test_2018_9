package com.osmolovskyi.test.actors

import akka.actor.{Actor, Props}
import com.osmolovskyi.test.actors.SlaServiceActor._
import com.osmolovskyi.test.models._
import com.osmolovskyi.test.services.SlaService
import com.typesafe.scalalogging.LazyLogging
import javax.inject.{Inject, Singleton}
import play.api.Configuration
import play.api.cache.{AsyncCacheApi, NamedCache}

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._

@Singleton
class SlaServiceActor(slaService: SlaService,
                      @NamedCache("user-token-cache") cache: AsyncCacheApi)
                      (implicit configuration: Configuration,
                       ec: ExecutionContext) extends Actor with LazyLogging {

  override def receive: Receive = receiveWithTokenBuffer(Set.empty)

  private def receiveWithTokenBuffer(tokenBuffer: Set[String]): Receive = {
    case GetSla(token) if !tokenBuffer.contains(token) =>
      val sndr = sender()
      cache.get[Sla](token).flatMap {
        case Some(sla) => Future.successful(sla)
        case None => slaService.getSlaByToken(token)
      }.flatMap { resp =>
        cache.set(token, resp).map { _ =>
          sndr ! resp
          self ! SlaReceived(token)
          resp
        }
      }

      context.become(receiveWithTokenBuffer(tokenBuffer + token))

    case getSla: GetSla =>
      context.system.scheduler.scheduleOnce(50.millis, self, getSla)

    case SlaReceived(token) =>
      context.become(receiveWithTokenBuffer(tokenBuffer - token))

    case message =>
      logger.error(s"Received unexpected message: $message")
  }
}

object SlaServiceActor {

  def props(slaService: SlaService,
            @NamedCache("user-token-cache") cache: AsyncCacheApi)
           (implicit configuration: Configuration, ec: ExecutionContext): Props =
    Props(new SlaServiceActor(slaService, cache)).withDispatcher("mailboxes.sla-dispatcher")

  case class GetSla(token: String)

  case class SlaReceived(token: String)


}

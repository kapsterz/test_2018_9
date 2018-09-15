package com.osmolovskyi.test.actors

import akka.actor.Actor
import com.osmolovskyi.test.actors.SlaServiceActor._
import com.osmolovskyi.test.models._
import com.osmolovskyi.test.services.SlaService
import com.typesafe.scalalogging.LazyLogging
import javax.inject.{Inject, Singleton}
import play.api.Configuration

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SlaServiceActor @Inject()(configuration: Configuration,
                                slaService: SlaService)
                               (implicit ec: ExecutionContext) extends Actor with LazyLogging {

  override def receive: Receive = receiveWithTokenBuffer(Set.empty)

  private def receiveWithTokenBuffer(tokenBuffer: Set[Buffer]): Receive = {
    case GetSla(token) if tokenBuffer.forall(_.token != token) =>
      val response = slaService.getSlaByToken(token).map { resp =>
        sender() ! resp
        self ! SlaReceived(token)
        resp
      }
      context.become(receiveWithTokenBuffer(tokenBuffer + Buffer(token, response)))

    case GetSla(token) =>
      tokenBuffer.find(_.token == token).get.response.map(sender() ! _)

    case SlaReceived(token) =>
      context.become(receiveWithTokenBuffer(tokenBuffer.filter(_.token == token)))

    case message =>
      logger.error(s"Received unexpected message: $message")
  }
}

object SlaServiceActor {

  case class GetSla(token: String)

  case class SlaReceived(token: String)

  case class Buffer(token: String, response: Future[Sla])

}

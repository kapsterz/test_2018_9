package com.osmolovskyi.test.services

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.pattern.ask
import akka.util.Timeout
import com.osmolovskyi.test.actors.SlaServiceActor
import com.osmolovskyi.test.helpers.DefaultValues
import com.osmolovskyi.test.models.Sla
import com.typesafe.scalalogging.LazyLogging
import javax.inject.Inject
import play.api.Configuration
import play.api.cache.AsyncCacheApi
import play.cache.NamedCache

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._

class SlaHelperService @Inject()(@NamedCache("user-token-cache") cache: AsyncCacheApi,
                                 as: ActorSystem,
                                 configuration: Configuration)
                                (implicit ec: ExecutionContext) extends LazyLogging {

  private implicit val timeout: Timeout =
    configuration
      .getOptional[Int]("slaService.defaultTimeout")
      .getOrElse(DefaultValues.defaultTimeout)
      .seconds

  private lazy val slaServiceActor: ActorRef = as.actorOf(Props[SlaServiceActor])

  def getSlaByToken(token: String): Future[Sla] = cache.get[Sla](token).flatMap {
    case Some(sla) => Future.successful(sla)
    case None => (slaServiceActor ? SlaServiceActor.GetSla(token)).flatMap {
      case sla: Sla =>
        cache.set(token, sla).map(_ => sla)

      case el =>
        throw new IllegalArgumentException(s"Exeption during fetching SLA, recieved item is: $el")
    }
  }
}

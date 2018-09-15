package com.osmolovskyi.test.services

import akka.actor.{ActorRef, ActorSystem}
import akka.pattern.ask
import akka.util.Timeout
import com.osmolovskyi.test.actors.SlaServiceActor
import com.osmolovskyi.test.helpers.DefaultValues
import com.osmolovskyi.test.models.Sla
import com.typesafe.scalalogging.LazyLogging
import javax.inject.{Inject, Singleton}
import play.api.Configuration
import play.api.cache.AsyncCacheApi
import play.cache.NamedCache

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SlaHelperService @Inject()(@NamedCache("user-token-cache") cache: AsyncCacheApi,
                                 as: ActorSystem,
                                 slaService: SlaService)
                                (implicit configuration: Configuration) extends LazyLogging {

  private implicit lazy val ec: ExecutionContext = as.dispatcher

  private implicit val timeout: Timeout =
    configuration
      .getOptional[Int]("slaService.defaultTimeout")
      .getOrElse(DefaultValues.defaultTimeout)
      .seconds

  private lazy val slaServiceActor: ActorRef = as.actorOf(SlaServiceActor.props(slaService, cache), "slaServiceActor")

  def getSlaByToken(token: String): Future[Sla] =
    (slaServiceActor ? SlaServiceActor.GetSla(token)).map {
      case sla: Sla =>
        sla
      case el =>
        throw new IllegalArgumentException(s"Exeption during fetching SLA, recieved item is: $el")
    }
}

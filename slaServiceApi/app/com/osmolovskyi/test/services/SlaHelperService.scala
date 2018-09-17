package com.osmolovskyi.test.services

import akka.actor.ActorSystem
import akka.util.Timeout
import com.osmolovskyi.test.helpers.DefaultConfigs
import com.osmolovskyi.test.models.Sla
import com.typesafe.scalalogging.LazyLogging
import javax.inject.{Inject, Singleton}
import play.api.cache.AsyncCacheApi
import play.cache.NamedCache

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SlaHelperService @Inject()(@NamedCache("user-token-cache") cache: AsyncCacheApi,
                                 as: ActorSystem,
                                 slaService: SlaService,
                                 defaultConfigs: DefaultConfigs)
                                (implicit ec: ExecutionContext) extends LazyLogging {

  private implicit val timeout: Timeout = 30.seconds

  def getSlaByToken(token: String): Future[Sla] =
    cache.get[Sla](token).flatMap {
      case Some(sla) =>
        Future.successful(sla)
      case None =>
        slaService.getSlaByToken(token)
          .flatMap { sla =>
            cache.set(token, sla).map(_ => sla)
          }
    }
}

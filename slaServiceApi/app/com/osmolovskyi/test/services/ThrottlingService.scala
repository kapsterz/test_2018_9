package com.osmolovskyi.test.services

import java.util.UUID

import akka.actor.{ActorRef, ActorSystem}
import akka.pattern.ask
import akka.util.Timeout
import com.osmolovskyi.test.actors.UserActor
import com.osmolovskyi.test.helpers.DefaultConfigs
import com.osmolovskyi.test.helpers.ImplicitsHelper._
import com.typesafe.scalalogging.LazyLogging
import javax.inject.{Inject, Singleton}
import play.api.cache.AsyncCacheApi
import play.cache.NamedCache

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try


sealed trait ThrottlingService {
  val graceRps: Int // configurable
  val slaService: SlaService // use mocks/stubs for testing
  // Should return true if the request is within allowed RPS.
  def isRequestAllowed(token: Option[String]): Boolean
}

@Singleton
class ThrottlingServiceImpl @Inject()(@NamedCache("user-actor-cache") asyncCacheApi: AsyncCacheApi,
                                      slaHelperService: SlaHelperService,
                                      slaServiceImpl: SlaService,
                                      as: ActorSystem,
                                      defaultConfigs: DefaultConfigs) extends ThrottlingService with LazyLogging {
  private implicit lazy val ec: ExecutionContext = as.dispatcher
  private lazy val unauthorizedUser: ActorRef = as.actorOf(UserActor.props(graceRps), "unauthorized")
  private implicit val timeout: Timeout = defaultConfigs.defaultTimeout


  val slaService: SlaService = slaServiceImpl
  val graceRps: Int = defaultConfigs.graceRps

  def isRequestAllowed(tokenOpt: Option[String]): Boolean = {
    val id = UUID.randomUUID()
    val isAllowed = (unauthorizedUser ? UserActor.RequestInProgress(id)).flatMap {
      case true =>
        tokenOpt match {
          case Some(token) =>
            for {
              sla <- slaHelperService.getSlaByToken(token)
              userActorOpt <- asyncCacheApi.get[ActorRef](sla.user)
              userActor <- userActorOpt.map(Future.successful).getOrElse {
                val newUserActor = Try(as.actorOf(UserActor.props(sla.rps), sla.user)).getOrElse(
                  as.actorSelection(s"/user/${sla.user}").resolveOne().waitFor
                )
                asyncCacheApi.set(sla.user, newUserActor).map { _ =>
                  logger.debug(s"Created new actor for user $token")
                  newUserActor
                }
              }
              result <- processRequest(userActor, id, sla.user, token)
            } yield result

          case None =>
            logger.debug(s"Request for unauthorized user processed successfully")
            Future.successful(true)
        }
      case _ =>
        logger.debug(s"Request for unauthorized user doesn't processed, limit exceeded")
        Future.successful(false)
    }

    isAllowed.waitFor
  }

  private def processRequest(userActor: ActorRef, id: UUID, user: String, token: String): Future[Boolean] = {
    unauthorizedUser ! UserActor.RequestForceProcessed(id)
    (userActor ? UserActor.RequestInProgress(id)).map {
      case true =>
        logger.debug(s"Request for user $user with token $token processed successfully")
        true
      case _ =>
        logger.debug(s"Request for user $user with token $token doesn't processed, limit exceeded")
        false
    }
  }
}

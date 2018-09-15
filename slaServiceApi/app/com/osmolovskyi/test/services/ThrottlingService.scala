package com.osmolovskyi.test.services

import java.util.UUID

import akka.actor.{ActorRef, ActorSystem}
import akka.pattern.ask
import akka.util.Timeout
import com.osmolovskyi.test.actors.UserActor
import com.osmolovskyi.test.helpers.DefaultValues
import com.typesafe.scalalogging.LazyLogging
import javax.inject.Inject
import play.api.Configuration
import play.api.cache.AsyncCacheApi
import play.cache.NamedCache

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}


sealed trait ThrottlingService {
  val graceRps: Int // configurable
  val slaService: SlaService // use mocks/stubs for testing
  // Should return true if the request is within allowed RPS.
  def isRequestAllowed(token: Option[String]): Boolean
}

class ThrottlingServiceImpl @Inject()(@NamedCache("userCache") asyncCacheApi: AsyncCacheApi,
                                      configuration: Configuration,
                                      slaHelperService: SlaHelperService,
                                      as: ActorSystem) extends ThrottlingService with LazyLogging {
  private implicit lazy val ec: ExecutionContext = as.dispatcher
  val slaService: SlaService = ???
  private implicit val timeout: Timeout =
    configuration
      .getOptional[Int]("slaService.defaultTimeout")
      .getOrElse(DefaultValues.defaultTimeout)
      .seconds

  val graceRps: Int = configuration.getOptional[Int]("app.user.graceRps").getOrElse(DefaultValues.defaultGraceRps)

  private lazy val unauthorizedUser: ActorRef = as.actorOf(UserActor.props(graceRps), "unauthorized")

  def isRequestAllowed(tokenOpt: Option[String]): Boolean = {
    val id = UUID.randomUUID()

    val isAllowed = (unauthorizedUser ? UserActor.RequestInProgress(id)).flatMap {
      case true =>
        tokenOpt match {
          case Some(token) =>
            for {
              sla <- slaHelperService.getSlaByToken(token)
              userActorOpt <- asyncCacheApi.get[ActorRef](sla.user)
              userActor <- userActorOpt.map(Future.successful).getOrElse{
                val newUserActor = as.actorOf(UserActor.props(sla.rps), sla.user)
                asyncCacheApi.set(sla.user, newUserActor).map { _ =>
                  logger.info(s"Created new actor for user ${sla.user}")
                  newUserActor
                }
              }
              result <- processRequest(userActor, id, sla.user, token)
            } yield result

          case None =>
            logger.info(s"Request for unauthorized user processed successfully")
            Future.successful(true)
        }
      case _ =>
        logger.warn(s"Request for unauthorized user doesn't processed, limit exceeded")
        Future.successful(false)
    }

    Await.result(isAllowed, DefaultValues.defaultTimeout.seconds)
  }


  private def processRequest(userActor: ActorRef, id: UUID, user: String, token: String): Future[Boolean] = {
    unauthorizedUser ! UserActor.RequestForceProcessed(id)
    (userActor ? UserActor.RequestInProgress(id)).map {
      case true =>
        logger.info(s"Request for user $user with token $token processed successfully")
        true
      case _ =>
        logger.warn(s"Request for user $user with token $token doesn't processed, limit exceeded")
        false
    }
  }
}

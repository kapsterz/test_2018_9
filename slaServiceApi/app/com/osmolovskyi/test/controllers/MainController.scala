package com.osmolovskyi.test.controllers

import com.osmolovskyi.test.services.ThrottlingService
import com.typesafe.scalalogging.LazyLogging
import javax.inject._
import play.api.cache.{AsyncCacheApi, NamedCache}
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class MainController @Inject()(val loadTestController: LoadTestController,
                               @NamedCache("user-actor-cache") val userActorCache: AsyncCacheApi,
                               @NamedCache("user-token-cache") val userTokenCache: AsyncCacheApi,
                               cc: ControllerComponents,
                               throttlingService: ThrottlingService)
                              (implicit exec: ExecutionContext) extends AbstractController(cc) with LazyLogging {

  def test(token: Option[String]): Action[AnyContent] = Action.async {
    Future(Ok(throttlingService.isRequestAllowed(token.filterNot(_.isEmpty)).toString))
  }

  def testWithoutSLA(token: Option[String]): Action[AnyContent] = Action.async {
    Future(Ok("true"))
  }

  def ensureCaches(): Action[AnyContent] = Action.async {
    logger.info("Cache ensuring successfully started")
    userActorCache.removeAll().flatMap(_ => userTokenCache.removeAll()).map(_ => Ok("Done"))
  }

}

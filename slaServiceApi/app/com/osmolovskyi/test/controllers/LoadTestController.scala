package com.osmolovskyi.test.controllers

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpMethods.GET
import akka.http.scaladsl.model._
import akka.stream.Materializer
import com.osmolovskyi.test.helpers.DefaultConfigs
import com.osmolovskyi.test.services.LoadTestService
import com.typesafe.scalalogging.LazyLogging
import javax.inject.{Inject, Singleton}
import play.api.Configuration
import play.api.inject.ApplicationLifecycle

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class LoadTestController @Inject()(configuration: Configuration,
                                   lifecycle: ApplicationLifecycle,
                                   loadTestService: LoadTestService,
                                   defaultConfigs: DefaultConfigs)
                                  (implicit as: ActorSystem,
                                   ex: ExecutionContext,
                                   mat: Materializer) extends LazyLogging {

  logger.info("Loading LoadTest module")

  private val httpHandler: HttpRequest => Future[HttpResponse] = {

    case HttpRequest(GET, Uri.Path("/api/start"), _, _, _) =>
      loadTestService.start

    case _: HttpRequest =>
      Future.successful(HttpResponse(status = StatusCodes.NotFound, entity = "Unknown resource!"))
  }

  private val http: Future[Http.ServerBinding] =
    Http().bindAndHandleAsync(httpHandler, "0.0.0.0", defaultConfigs.LoadTest.port)

  lifecycle.addStopHook { () =>
    Future.successful({
      logger.info("Stopping LoadTest module...")
      http.flatMap(_.unbind())
    })
  }

}
package com.osmolovskyi.test.controllers

import akka.actor.ActorSystem
import com.osmolovskyi.test.services.ThrottlingService
import javax.inject._
import play.api.mvc._

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future, Promise}

@Singleton
class MainController @Inject()(cc: ControllerComponents,
                               actorSystem: ActorSystem,
                               throttlingService: ThrottlingService)
                              (implicit exec: ExecutionContext) extends AbstractController(cc) {

  def test(token: Option[String]) = Action {
    Ok(throttlingService.isRequestAllowed(token).toString)
  }

  private def getFutureMessage(delayTime: FiniteDuration): Future[String] = {
    val promise: Promise[String] = Promise[String]()
    actorSystem.scheduler.scheduleOnce(delayTime) {
      promise.success("Hi!")
    }(actorSystem.dispatcher) // run scheduled tasks using the actor system's dispatcher
    promise.future
  }

}

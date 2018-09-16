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

}

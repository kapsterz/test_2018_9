package com.osmolovskyi.test.actors

import java.util.UUID

import akka.actor.{ActorRef, ActorSystem}
import akka.testkit.{ImplicitSender, TestKit}
import org.scalamock.scalatest.MockFactory
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}
import org.scalatestplus.play.guice.GuiceOneAppPerTest
import play.api.Application

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

class UserActorSpec extends TestKit(ActorSystem("SlaServiceActorSpec")) with ImplicitSender
  with WordSpecLike with Matchers with BeforeAndAfterAll with GuiceOneAppPerTest with MockFactory {

  override def afterAll: Unit = {
    TestKit.shutdownActorSystem(system)
  }

  "User Actor" should {

    "correctly return status for 1 request" in new UserActorScope(app) {
      val slaServiceActor: ActorRef = system.actorOf(UserActor.props(defaultRps))
      slaServiceActor ! UserActor.RequestInProgress(uuid)
      expectMsg(defaultTimeout, true)
    }

    "correctly return status for 30 request" in new UserActorScope(app) {
      val slaServiceActor: ActorRef = system.actorOf(UserActor.props(defaultRps))
      (1 to 30).map { _ =>
        slaServiceActor ! UserActor.RequestInProgress(uuid)
        expectMsg(defaultTimeout, true)
      }
    }

    "correctly return status if limit exceeded" in new UserActorScope(app) {
      val slaServiceActor: ActorRef = system.actorOf(UserActor.props(defaultRps))
      (1 to 30).map { _ =>
        slaServiceActor ! UserActor.RequestInProgress(uuid)
        expectMsg(defaultTimeout, true)
      }
      slaServiceActor ! UserActor.RequestInProgress(uuid)
      expectMsg(defaultTimeout, false)
    }

    "increase limit after 0.1 second" in new UserActorScope(app) {
      val slaServiceActor: ActorRef = system.actorOf(UserActor.props(defaultRps))
      (1 to 30).map { _ =>
        slaServiceActor ! UserActor.RequestInProgress(uuid)
        expectMsg(defaultTimeout, true)
      }
      slaServiceActor ! UserActor.RequestInProgress(uuid)
      expectMsg(defaultTimeout, false)
      Thread.sleep(120)
      (1 to 3).map { _ =>
        slaServiceActor ! UserActor.RequestInProgress(uuid)
        expectMsg(defaultTimeout, true)
      }
    }

    "correctly process force request processing" in new UserActorScope(app) {
      val slaServiceActor: ActorRef = system.actorOf(UserActor.props(defaultRps))
      (1 to 30).map { _ =>
        slaServiceActor ! UserActor.RequestInProgress(uuid)
        expectMsg(defaultTimeout, true)
      }
      slaServiceActor ! UserActor.RequestInProgress(uuid)
      expectMsg(defaultTimeout, false)

      (1 to 3).map { _ =>
        slaServiceActor ! UserActor.RequestForceProcessed(uuid)
        slaServiceActor ! UserActor.RequestInProgress(uuid)
        expectMsg(defaultTimeout, true)
      }
    }

    "correctly return status for 10000 requests" in new UserActorScope(app) {
      val slaServiceActor: ActorRef = system.actorOf(UserActor.props(incRps))
      (1 to 10000).map { _ =>
        slaServiceActor ! UserActor.RequestInProgress(uuid)
        expectMsg(defaultTimeout, true)
      }
      slaServiceActor ! UserActor.RequestInProgress(uuid)
      expectMsg(defaultTimeout, false)
    }

  }
}

abstract class UserActorScope(application: Application) {
  implicit lazy val ec: ExecutionContext = application.injector.instanceOf[ExecutionContext]
  val defaultRps = 30
  val incRps = 10000
  val defaultTimeout: FiniteDuration = 30.seconds
  def uuid: UUID = UUID.randomUUID()
}

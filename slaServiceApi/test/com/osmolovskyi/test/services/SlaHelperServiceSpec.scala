package com.osmolovskyi.test.services

import akka.actor.ActorSystem
import akka.util.Timeout
import com.osmolovskyi.test.helpers.ImplicitsHelper._
import com.osmolovskyi.test.helpers.{DefaultConfigs, SlaHelper, SlaServiceMock}
import com.osmolovskyi.test.models.Sla
import org.scalamock.scalatest.MockFactory
import org.scalatest.{Matchers, WordSpec}
import org.scalatestplus.play.guice.GuiceOneAppPerTest
import play.api.cache.AsyncCacheApi
import play.api.{Application, Configuration}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.util.Random

class SlaHelperServiceSpec extends WordSpec with Matchers with GuiceOneAppPerTest with MockFactory with SlaServiceMock {

  "SLA Helper Service" should {

    "correctly return SLA info for 1 token" in new SlaHelperServiceScope(app) {
      val slaService: SlaService = slaServiceMock(firstSla)
      val slaHelperService: SlaHelperService = new SlaHelperService(cache, as, slaService, defaultConfigs)
      slaHelperService.getSlaByToken(firstToken).waitFor shouldEqual firstSla
    }

    "correctly return SLA info for 2 different tokens" in new SlaHelperServiceScope(app) {
      val slaService: SlaService = slaServiceMock(firstSla)
      val slaHelperService: SlaHelperService = new SlaHelperService(cache, as, slaService, defaultConfigs)
      slaHelperService.getSlaByToken(firstToken).waitFor shouldEqual firstSla
      slaHelperService.getSlaByToken(secondToken).waitFor shouldEqual firstSla
    }

    "correctly return SLA info for 2 different SLA" in new SlaHelperServiceScope(app) {
      val slaService: SlaService = slaServiceMockMultiple(dataMap)
      val slaHelperService: SlaHelperService = new SlaHelperService(cache, as, slaService, defaultConfigs)
      slaHelperService.getSlaByToken(firstToken).waitFor shouldEqual firstSla
      slaHelperService.getSlaByToken(secondToken).waitFor shouldEqual secondSla
    }

    s"correctly return SLA info 10000 times" in new SlaHelperServiceScope(app) {
      val slaService: SlaService = slaServiceMockMultiple(dataMap)
      val slaHelperService: SlaHelperService = new SlaHelperService(cache, as, slaService, defaultConfigs)
      (1 to 10000).map { _ =>
        slaHelperService.getSlaByToken(firstToken).waitFor shouldEqual firstSla
        slaHelperService.getSlaByToken(secondToken).waitFor shouldEqual secondSla
      }
    }

    s"correctly return SLA info 10000 times for 1000 customers" in new SlaHelperServiceScope(app) {
      val slaService: SlaService = slaServiceMockMultiple(dataMapRandom)
      val slaHelperService: SlaHelperService = new SlaHelperService(cache, as, slaService, defaultConfigs)
      dataMapRandom.keys.map { token =>
        (1 to 10).map { _ =>
          slaHelperService.getSlaByToken(token).waitFor shouldEqual dataMapRandom(token)
        }
        cache.get[Sla](token).waitFor.isDefined shouldEqual true
      }
    }

  }
}

abstract class SlaHelperServiceScope(application: Application) {
  lazy val as: ActorSystem = application.actorSystem
  implicit lazy val ec: ExecutionContext = application.actorSystem.dispatcher
  implicit val timeout: Timeout = 10.seconds
  lazy val cache: AsyncCacheApi = application.injector.instanceOf[AsyncCacheApi]
  lazy val slaHelper: SlaHelper = new SlaHelper
  lazy val defaultConfigs = new DefaultConfigs(Configuration("slaService.defaultTimeout" -> 30))
  val firstSla: Sla = Sla("first", 10)
  val secondSla: Sla = Sla("second", 10)
  val firstToken = "first"
  val secondToken = "second"
  val dataMap: Map[String, Sla] = Map(firstToken -> firstSla, secondToken -> secondSla)
  val dataMapRandom: Map[String, Sla] =
    slaHelper.newUserPool(1000, 10, 10)
      .map(Random.alphanumeric.take(10).mkString -> _).toMap
}


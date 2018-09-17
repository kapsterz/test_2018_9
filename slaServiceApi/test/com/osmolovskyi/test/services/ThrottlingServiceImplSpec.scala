package com.osmolovskyi.test.services

import akka.actor.ActorSystem
import com.osmolovskyi.test.helpers.{DefaultConfigs, SlaHelper, SlaHelperServiceMock, SlaServiceMock}
import com.osmolovskyi.test.models.Sla
import org.scalamock.scalatest.MockFactory
import org.scalatest.{Matchers, TestData, WordSpec}
import org.scalatestplus.play.guice.GuiceOneAppPerTest
import play.api.cache.AsyncCacheApi
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.{Application, Configuration}

import scala.concurrent.duration._
import scala.util.Random

class ThrottlingServiceImplSpec extends WordSpec with Matchers with GuiceOneAppPerTest
  with MockFactory with SlaServiceMock with SlaHelperServiceMock {
  override def newAppForTest(testData: TestData): Application = GuiceApplicationBuilder(
    configuration = Configuration("slaService.graceRps" -> 100000)
  ).build()

  "SLA Helper Service for unauthorized user" should {

    "correctly return status for 1 token" in new ThrottlingServiceImplScope(app) {
      val slaService: SlaService = slaServiceMock(firstSla)
      val slaHelperService: SlaHelperService = slaHelperServiceMock(firstSla)
      val throttlingServiceImpl: ThrottlingServiceImpl =
        new ThrottlingServiceImpl(cache, slaHelperService, slaService, as, defaultConfigs)
      throttlingServiceImpl.isRequestAllowed(None) shouldEqual true
    }

    "correctly return status for 30 request" in new ThrottlingServiceImplScope(app) {
      val slaService: SlaService = slaServiceMock(firstSla)
      val slaHelperService: SlaHelperService = slaHelperServiceMock(firstSla)
      val throttlingServiceImpl: ThrottlingServiceImpl =
        new ThrottlingServiceImpl(cache, slaHelperService, slaService, as, defaultConfigs)
      (1 to 30).map { _ =>
        throttlingServiceImpl.isRequestAllowed(None) shouldEqual true
      }
    }

    "correctly return status if limit exceeded" in new ThrottlingServiceImplScope(app) {
      val slaService: SlaService = slaServiceMock(firstSla)
      val slaHelperService: SlaHelperService = slaHelperServiceMock(firstSla)
      val throttlingServiceImpl: ThrottlingServiceImpl =
        new ThrottlingServiceImpl(cache, slaHelperService, slaService, as, defaultConfigs)
      (1 to 30).map { _ =>
        throttlingServiceImpl.isRequestAllowed(None) shouldEqual true
      }
      throttlingServiceImpl.isRequestAllowed(None) shouldEqual false
    }

    "increase limit after 0.1 second" in new ThrottlingServiceImplScope(app) {
      val slaService: SlaService = slaServiceMock(firstSla)
      val slaHelperService: SlaHelperService = slaHelperServiceMock(firstSla)
      val throttlingServiceImpl: ThrottlingServiceImpl =
        new ThrottlingServiceImpl(cache, slaHelperService, slaService, as, defaultConfigs)
      (1 to 30).map { _ =>
        throttlingServiceImpl.isRequestAllowed(None) shouldEqual true
      }
      throttlingServiceImpl.isRequestAllowed(None) shouldEqual false
      Thread.sleep(150)
      (1 to 3).map { _ =>
        throttlingServiceImpl.isRequestAllowed(None) shouldEqual true
      }
    }
  }

  "SLA Helper Service for authorized user" should {

    "correctly return status for 1 token" in new ThrottlingServiceImplScope(app) {
      val slaService: SlaService = slaServiceMock(firstSla)
      val slaHelperService: SlaHelperService = slaHelperServiceMock(firstSla)
      val throttlingServiceImpl: ThrottlingServiceImpl =
        new ThrottlingServiceImpl(cache, slaHelperService, slaService, as, defaultConfigs)
      throttlingServiceImpl.isRequestAllowed(Some(firstToken)) shouldEqual true
    }

    "correctly return status for 30 request" in new ThrottlingServiceImplScope(app) {
      val slaService: SlaService = slaServiceMock(firstSla)
      val slaHelperService: SlaHelperService = slaHelperServiceMock(firstSla)
      val throttlingServiceImpl: ThrottlingServiceImpl =
        new ThrottlingServiceImpl(cache, slaHelperService, slaService, as, defaultConfigs)
      (1 to 30).map { _ =>
        throttlingServiceImpl.isRequestAllowed(Some(firstToken)) shouldEqual true
      }
    }

    "correctly return status if limit exceeded" in new ThrottlingServiceImplScope(app) {
      val slaService: SlaService = slaServiceMock(firstSla)
      val slaHelperService: SlaHelperService = slaHelperServiceMock(firstSla)
      val throttlingServiceImpl: ThrottlingServiceImpl =
        new ThrottlingServiceImpl(cache, slaHelperService, slaService, as, defaultConfigs)
      (1 to 30).map { _ =>
        throttlingServiceImpl.isRequestAllowed(Some(firstToken)) shouldEqual true
      }
      throttlingServiceImpl.isRequestAllowed(Some(firstToken)) shouldEqual false
    }

    "increase limit after 0.1 second" in new ThrottlingServiceImplScope(app) {
      val slaService: SlaService = slaServiceMock(firstSla)
      val slaHelperService: SlaHelperService = slaHelperServiceMock(firstSla)
      val throttlingServiceImpl: ThrottlingServiceImpl =
        new ThrottlingServiceImpl(cache, slaHelperService, slaService, as, defaultConfigs)
      (1 to 30).map { _ =>
        throttlingServiceImpl.isRequestAllowed(Some(firstToken)) shouldEqual true
      }
      throttlingServiceImpl.isRequestAllowed(Some(firstToken)) shouldEqual false
      Thread.sleep(120)
      (1 to 3).map { _ =>
        throttlingServiceImpl.isRequestAllowed(Some(firstToken)) shouldEqual true
      }
    }
  }

  "SLA Helper Service for 1000 users" should {

    "correctly return status" in new ThrottlingServiceImplScope(app) {
      val slaService: SlaService = slaServiceMockMultiple(dataMapRandom)
      val slaHelperService: SlaHelperService = slaHelperServiceMockMultiple(dataMapRandom)
      val throttlingServiceImpl: ThrottlingServiceImpl =
        new ThrottlingServiceImpl(cache, slaHelperService, slaService, as, changedConfigs)

      dataMapRandom.keys.map { token =>
        throttlingServiceImpl.isRequestAllowed(Some(token)) shouldEqual true
      }
    }

    "correctly return status if limit exceeded" in new ThrottlingServiceImplScope(app) {
      val slaService: SlaService = slaServiceMockMultiple(dataMapRandom)
      val slaHelperService: SlaHelperService = slaHelperServiceMockMultiple(dataMapRandom)
      val throttlingServiceImpl: ThrottlingServiceImpl =
        new ThrottlingServiceImpl(cache, slaHelperService, slaService, as, changedConfigs)
      dataMapRandom.keys.map { token =>
        (1 to 30).map { _ =>
          throttlingServiceImpl.isRequestAllowed(Some(token)) shouldEqual true
        }
        throttlingServiceImpl.isRequestAllowed(Some(token)) shouldEqual false
      }
    }

    "correctly return status 30 times per user" in new ThrottlingServiceImplScope(app) {
      val slaService: SlaService = slaServiceMockMultiple(dataMapRandom)
      val slaHelperService: SlaHelperService = slaHelperServiceMockMultiple(dataMapRandom)
      val throttlingServiceImpl: ThrottlingServiceImpl =
        new ThrottlingServiceImpl(cache, slaHelperService, slaService, as, changedConfigs)
      (1 to 30).map { _ =>
        dataMapRandom.keys.map { token =>
          throttlingServiceImpl.isRequestAllowed(Some(token)) shouldEqual true
        }
      }
    }

    "increase limit after 0.1 second" in new ThrottlingServiceImplScope(app) {
      val slaService: SlaService = slaServiceMockMultiple(dataMapRandom)
      val slaHelperService: SlaHelperService = slaHelperServiceMockMultiple(dataMapRandom)
      val throttlingServiceImpl: ThrottlingServiceImpl =
        new ThrottlingServiceImpl(cache, slaHelperService, slaService, as, changedConfigs)
      dataMapRandom.keys.map { token =>
        (1 to 30).map { _ =>
          throttlingServiceImpl.isRequestAllowed(Some(token)) shouldEqual true
        }
        throttlingServiceImpl.isRequestAllowed(Some(token)) shouldEqual false
      }
      Thread.sleep(120)
      (1 to 3).map { _ =>
        dataMapRandom.keys.map { token =>
          throttlingServiceImpl.isRequestAllowed(Some(token)) shouldEqual true
        }
      }
    }
  }
}

abstract class ThrottlingServiceImplScope(application: Application) {
  lazy val as: ActorSystem = application.injector.instanceOf[ActorSystem]
  lazy val cache: AsyncCacheApi = application.injector.instanceOf[AsyncCacheApi]
  lazy val slaHelper: SlaHelper = new SlaHelper
  val defaultConfig: Configuration = Configuration("slaService.defaultTimeout" -> 30, "slaService.graceRps" -> 30)
  val defaultConfigs: DefaultConfigs = new DefaultConfigs(defaultConfig)
  val changedConfig = Configuration("slaService.defaultTimeout" -> 30, "slaService.graceRps" -> 10000)
  val changedConfigs: DefaultConfigs = new DefaultConfigs(changedConfig)
  val defaultTimeout: FiniteDuration = 30.seconds
  val firstSla: Sla = Sla("first", 30)
  val secondSla: Sla = Sla("second", 10)
  val firstToken = "first"
  val secondToken = "second"
  val dataMap: Map[String, Sla] = Map(firstToken -> firstSla, secondToken -> secondSla)
  val dataMapRandom: Map[String, Sla] =
    slaHelper.newUserPool(1000, 10, 30)
      .map(Random.alphanumeric.take(10).mkString -> _.copy(rps = 30)).toMap
}

package com.osmolovskyi.test.services

import akka.util.Timeout
import com.osmolovskyi.test.helpers.ImplicitsHelper._
import com.osmolovskyi.test.helpers.{DefaultConfigs, SlaHelper, SlaHelperMock}
import com.osmolovskyi.test.models.Sla
import org.scalamock.scalatest.MockFactory
import org.scalatest.{Matchers, WordSpec}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.{Application, Configuration}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._


class SlaServiceSpec extends WordSpec with Matchers with GuiceOneAppPerSuite with MockFactory with SlaHelperMock {
  implicit override lazy val app: Application = GuiceApplicationBuilder(
    configuration = Configuration("slaService.user.slaResponseTime" -> 0, "slaService.user.count" -> 2)
  ).build()
  "SLA Service" should {
    "correctly return SLA info when equal tokens" in new SlaServiceScope(app) {
      val slaHelper: SlaHelper = slaHelperMock(data)
      val slaService: SlaServiceImpl = slaServiceImpl(slaHelper)
      slaService.getSlaByToken("111").waitFor shouldEqual firstSla
      slaService.getSlaByToken("111").waitFor shouldEqual secondSla
    }

    "correctly return SLA info when different tokens" in new SlaServiceScope(app) {
      val slaHelper: SlaHelper = slaHelperMock(data)
      val slaService: SlaServiceImpl = slaServiceImpl(slaHelper)
      slaService.getSlaByToken(firstToken).waitFor shouldEqual firstSla
      slaService.getSlaByToken(firstToken).waitFor shouldEqual secondSla
    }
    "correctly return SLA info when different tokens 10 times" in new SlaServiceScope(app) {
      val slaHelper: SlaHelper = slaHelperMockMultiple(dataMap)
      val slaService: SlaServiceImpl = slaServiceImpl(slaHelper)
      (1 to 10).map { _ =>
        slaService.getSlaByToken(firstToken).waitFor shouldEqual firstSla
        slaService.getSlaByToken(secondToken).waitFor shouldEqual secondSla
      }
    }
  }
}

abstract class SlaServiceScope(application: Application) {
  implicit lazy val ec: ExecutionContext = application.injector.instanceOf[ExecutionContext]
  lazy val defaultConfigs: DefaultConfigs = new DefaultConfigs(application.configuration)
  implicit val timeout: Timeout = 10.seconds
  val firstSla: Sla = Sla("first", 10)
  val secondSla: Sla = Sla("second", 10)
  val firstToken = "first"
  val secondToken = "second"
  val data: Seq[Sla] = Seq(firstSla, secondSla)
  val dataMap: Map[String, Sla] = Map(firstToken -> firstSla, secondToken -> secondSla)

  def slaServiceImpl(slaHelper: SlaHelper): SlaServiceImpl = new SlaServiceImpl(defaultConfigs, slaHelper)
}

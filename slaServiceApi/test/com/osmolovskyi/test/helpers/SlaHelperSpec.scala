package com.osmolovskyi.test.helpers

import com.osmolovskyi.test.models.Sla
import org.scalamock.scalatest.MockFactory
import org.scalatest.{Matchers, WordSpec}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application

class SlaHelperSpec extends WordSpec with Matchers with GuiceOneAppPerSuite with MockFactory {
  "SLA Helper" should {
    "correctly create SLA data pool" in new SlaHelperScope(app) {
      userPool.length shouldEqual userCount
      userPool.forall(_.rps == rps) shouldEqual true
      userPool.forall(_.user.length <= userNameLength) shouldEqual true
    }
  }
}

abstract class SlaHelperScope(application: Application) {
  lazy val userPool: Seq[Sla] = slaHelper.newUserPool(userCount, userNameLength, rps)
  val slaHelper: SlaHelper = new SlaHelper
  val userNameLength: Int = 10
  val rps: Int = 10
  val userCount: Int = 100
}

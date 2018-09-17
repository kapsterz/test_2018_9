package com.osmolovskyi.test.helpers

import com.osmolovskyi.test.models.Sla
import com.osmolovskyi.test.services.SlaService
import org.scalamock.scalatest.MockFactory

import scala.concurrent.Future

trait SlaServiceMock {
  mockFactory: MockFactory =>

  def slaServiceMock(data: Sla): SlaService = {
    val slaMock: SlaService = mock[SlaService]
    (slaMock.getSlaByToken(_: String)).expects(*).returning(Future.successful(data)).anyNumberOfTimes()
    slaMock
  }

  def slaServiceMockMultiple(data: Map[String, Sla]): SlaService = {
    val slaMock: SlaService = mock[SlaService]
    data.map { case (token, user) =>
      (slaMock.getSlaByToken(_: String)).expects(token).returning(Future.successful(user)).anyNumberOfTimes()
    }
    slaMock
  }
}

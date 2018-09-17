package com.osmolovskyi.test.helpers

import com.osmolovskyi.test.models.Sla
import com.osmolovskyi.test.services.SlaHelperService
import org.scalamock.scalatest.MockFactory

import scala.concurrent.Future

trait SlaHelperServiceMock {
  mockFactory: MockFactory =>

  def slaHelperServiceMock(data: Sla): SlaHelperService = {
    val slaMock: SlaHelperService = mock[SlaHelperService]
    (slaMock.getSlaByToken(_: String)).expects(*).returning(Future.successful(data)).anyNumberOfTimes()
    slaMock
  }

  def slaHelperServiceMockMultiple(data: Map[String, Sla]): SlaHelperService = {
    val slaMock: SlaHelperService = mock[SlaHelperService]
    data.map { case (token, user) =>
      (slaMock.getSlaByToken(_: String)).expects(token).returning(Future.successful(user)).anyNumberOfTimes()
    }
    slaMock
  }
}

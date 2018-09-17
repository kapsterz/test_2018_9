package com.osmolovskyi.test.helpers

import com.osmolovskyi.test.models.Sla
import org.scalamock.scalatest.MockFactory

trait SlaHelperMock {
  mockFactory: MockFactory =>

  def slaHelperMock(data: Seq[Sla]): SlaHelper = {
    val slaMock: SlaHelper = mock[SlaHelper]
    (slaMock.newUserPool(_: Int, _: Int, _: Int)).expects(*, *, *).returning(data)
    data.map(user =>
      (slaMock.nextUser(_: Seq[Sla], _: Int, _: String)).expects(*, *, *).returning(user).once())
    slaMock
  }

  def slaHelperMockMultiple(data: Map[String, Sla]): SlaHelper = {
    val slaMock: SlaHelper = mock[SlaHelper]
    (slaMock.newUserPool(_: Int, _: Int, _: Int)).expects(*, *, *).returning(data.values.toSeq)
    data.map { case (token, user) =>
      (slaMock.nextUser(_: Seq[Sla], _: Int, _: String)).expects(*, *, token).returning(user).anyNumberOfTimes()
    }
    slaMock
  }

}

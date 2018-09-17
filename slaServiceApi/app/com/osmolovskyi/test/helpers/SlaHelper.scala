package com.osmolovskyi.test.helpers

import com.osmolovskyi.test.models.Sla

import scala.util.Random

class SlaHelper {
  def newUserPool(userCount: Int, userNameLength: Int, rps: Int): Seq[Sla] =
    (1 to userCount).map { _ =>
      Sla(Random.alphanumeric.take(userNameLength).mkString, rps)
    }

  def nextUser(userPool: Seq[Sla], userCount: Int, token: String) = userPool(Random.nextInt(userCount))
}

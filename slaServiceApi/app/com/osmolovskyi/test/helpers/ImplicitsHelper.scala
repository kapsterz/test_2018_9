package com.osmolovskyi.test.helpers

import akka.util.Timeout

import scala.concurrent.{Await, Future}

object ImplicitsHelper {

  implicit class BooleanOps(boolean: Boolean) {
    def toInt: Int = if (boolean) 1 else 0
  }

  implicit class TestConcurrent[A](future: Future[A]) {
    def waitFor(implicit timeout: Timeout): A = {
      Await.result(future, timeout.duration)
    }
  }

}

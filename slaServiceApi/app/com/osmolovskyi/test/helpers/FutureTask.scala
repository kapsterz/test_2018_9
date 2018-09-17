package com.osmolovskyi.test.helpers

import java.util.{Timer, TimerTask}

import scala.concurrent.duration.Duration
import scala.concurrent._
import scala.util.Failure

class FutureTask[T](f: => Future[T]) extends TimerTask {
  val promise: Promise[T] = Promise[T]()

  def run(): Unit = promise.completeWith(f)

  override def cancel(): Boolean = {
    val result = super.cancel
    if (result) promise.complete(Failure(new CancellationException))
    result
  }

  def toFuture: Future[T] = this.promise.future
}

object FutureTask {

  private val defaultTimer = new java.util.Timer(true)

  def schedule[T](when: Duration)(f: => T)(implicit timer: Timer = defaultTimer, ctx: ExecutionContext): FutureTask[T] =
    scheduleFlat(when)(Future(f))(timer)

  def scheduleFlat[T](when: Duration)(f: => Future[T])(implicit timer: Timer = defaultTimer): FutureTask[T] = {
    val task = new FutureTask(f)
    timer.schedule(task, when.toMillis)
    task
  }
}

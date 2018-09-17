package com.osmolovskyi.test.services

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.model.{HttpEntity, HttpResponse}
import akka.pattern.ask
import akka.util.Timeout
import com.osmolovskyi.test.actors.LoadTestServiceActor
import com.osmolovskyi.test.actors.LoadTestServiceActor._
import com.osmolovskyi.test.helpers.{DefaultConfigs, FutureTask}
import com.osmolovskyi.test.models.StatisticReport
import com.typesafe.scalalogging.LazyLogging
import javax.inject.{Inject, Singleton}
import play.api.libs.json.Json

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class LoadTestService @Inject()(defaultConfigs: DefaultConfigs,
                                as: ActorSystem,
                                loadTestExecutionHelper: LoadTestExecutionService)
                               (implicit ec: ExecutionContext) extends LazyLogging {
  implicit lazy val timeout: Timeout = defaultConfigs.defaultTimeout + defaultConfigs.LoadTest.duration

  def start: Future[HttpResponse] = {
    logger.info("Received command for start executing loadtest")
    val loadTestServiceActor: ActorRef = as.actorOf(LoadTestServiceActor.props(loadTestExecutionHelper))

    loadTestServiceActor ! LoadTestServiceActor.BatchRequest

    FutureTask.scheduleFlat(defaultConfigs.LoadTest.duration) {
      loadTestServiceActor ? RequestStatisticReport
    }.toFuture.map {
      case report: StatisticReport =>
        val reportWithAdditionalInfo = report.copy(
          duration = Some(defaultConfigs.LoadTest.duration.toSeconds),
          requestsPerSecond = Some(report.requestsCount / defaultConfigs.LoadTest.duration.toSeconds))
        logger.info(s"Loadtest processed successfully, report was sent, report is: $reportWithAdditionalInfo")
        HttpResponse(entity = HttpEntity(Json.toJson(reportWithAdditionalInfo).toString))
      case el =>
        throw new IllegalArgumentException(s"Exception during fetching StatisticReport, received item is: $el")
    }
  }
}

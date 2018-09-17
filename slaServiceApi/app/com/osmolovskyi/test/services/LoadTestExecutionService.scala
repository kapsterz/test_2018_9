package com.osmolovskyi.test.services

import java.util.UUID

import akka.Done
import akka.actor.{ActorRef, ActorSystem}
import akka.pattern.ask
import akka.stream.scaladsl.{Sink, Source, SourceQueueWithComplete}
import akka.stream.{Materializer, OverflowStrategy, QueueOfferResult}
import akka.util.Timeout
import com.osmolovskyi.test.actors.StatisticActor
import com.osmolovskyi.test.actors.StatisticActor.Statistic
import com.osmolovskyi.test.helpers.DefaultConfigs
import com.osmolovskyi.test.models.StatisticReport
import com.typesafe.scalalogging.LazyLogging
import javax.inject.{Inject, Singleton}
import play.api.libs.ws.{WSClient, WSRequest}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Random

@Singleton
class LoadTestExecutionService @Inject()(defaultConfigs: DefaultConfigs,
                                         ws: WSClient,
                                         as: ActorSystem)
                                        (implicit ec: ExecutionContext,
                                         mat: Materializer) extends LazyLogging {

  private lazy val tokensPoolSize: Int = tokensPool.length
  private lazy val tokensPool: Seq[String] = (1 to defaultConfigs.userCount).map(_ => UUID.randomUUID().toString) ++ emptyTokensPool
  private lazy val emptyTokensPool: Seq[String] = (1 to defaultConfigs.userCount).map(_ => "")
  implicit lazy val timeout: Timeout = defaultConfigs.defaultTimeout

  private val queue: SourceQueueWithComplete[String] =
    Source
      .queue[String](defaultConfigs.LoadTest.QueueConfig.bufferSize, OverflowStrategy.backpressure)
      .mapAsync(defaultConfigs.LoadTest.QueueConfig.parallelism)(executeRequest)
      .to(Sink.ignore)
      .run()

  private val statisticActor: ActorRef = as.actorOf(StatisticActor.props())

  def executeQuery: Future[QueueOfferResult] = {
    val token = tokensPool(Random.nextInt(tokensPoolSize))
    queue.offer(token)
  }


  def getStatistic: Future[StatisticReport] = {
    (statisticActor ? StatisticActor.GetStatistic).map {
      case report: StatisticReport =>
        report
      case el =>
        throw new IllegalArgumentException(s"Exception during fetching StatisticReport, received item is: $el")
    }
  }


  private def executeRequest: PartialFunction[String, Future[Done]] = {
    case token => wsRequest(token).get().map { response =>
      statisticActor ! Statistic(response.status, response.body, token)
      Done
    }.recover {
      case ex =>
        logger.error("Error during executing request, exception is: ", ex)
        statisticActor ! Statistic(503, "false", token)
        Done
    }
  }

  private def wsRequest(token: String): WSRequest = {
    ws.url(s"${defaultConfigs.LoadTest.baseUrl}?token=$token")
  }

}

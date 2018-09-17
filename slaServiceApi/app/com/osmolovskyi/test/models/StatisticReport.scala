package com.osmolovskyi.test.models

import com.osmolovskyi.test.actors.StatisticActor._
import com.osmolovskyi.test.helpers.ImplicitsHelper._
import play.api.libs.json.{Json, OFormat}

case class StatisticReport(requestsCount: Int,
                           requestsSuccessful: Int,
                           requestsFailed: Int,
                           requestsAuthorizedFailed: Int,
                           requestsAuthorizedSuccessful: Int,
                           requestsUnauthorizedFailed: Int,
                           requestsUnauthorizedSuccessful: Int,
                           httpErrorsCount: Int,
                           duration: Option[Long] = None,
                           requestsPerSecond: Option[Long] = None) {

  def updateStatistics(statistic: Statistic): StatisticReport = {
    StatisticReport(
      requestsCount = requestsCount + 1,
      requestsSuccessful =
        requestsSuccessful + (statistic.isRequestAllowed == "true").toInt,
      requestsFailed =
        requestsFailed + (statistic.isRequestAllowed != "true").toInt,
      requestsAuthorizedFailed =
        requestsAuthorizedFailed + (statistic.isRequestAllowed != "true" && !statistic.token.isEmpty).toInt,
      requestsAuthorizedSuccessful =
        requestsAuthorizedSuccessful + (statistic.isRequestAllowed == "true" && !statistic.token.isEmpty).toInt,
      requestsUnauthorizedFailed =
        requestsUnauthorizedFailed + (statistic.isRequestAllowed != "true" && statistic.token.isEmpty).toInt,
      requestsUnauthorizedSuccessful =
        requestsUnauthorizedSuccessful + (statistic.isRequestAllowed == "true" && statistic.token.isEmpty).toInt,
      httpErrorsCount = httpErrorsCount + (statistic.status != 200).toInt)
  }
}

object StatisticReport {
  implicit val format: OFormat[StatisticReport] = Json.format[StatisticReport]

  def empty: StatisticReport = {
    StatisticReport(
      requestsCount = 0,
      requestsSuccessful = 0,
      requestsFailed = 0,
      requestsAuthorizedFailed = 0,
      requestsAuthorizedSuccessful = 0,
      requestsUnauthorizedFailed = 0,
      requestsUnauthorizedSuccessful = 0,
      httpErrorsCount = 0)
  }
}
package com.osmolovskyi.test.mailboxes

import akka.actor.ActorSystem
import akka.dispatch.{PriorityGenerator, UnboundedStablePriorityMailbox}
import com.osmolovskyi.test.actors.UserActor._
import com.typesafe.config.Config

class UserMailbox(settings: ActorSystem.Settings, config: Config)
  extends UnboundedStablePriorityMailbox(
    PriorityGenerator {

      case _: RequestInProgress => 3

      case _: RequestProcessed => 2

      case _: RequestForceProcessed => 1

      case _: LimitCounted => 0

      case _ => 4

    })


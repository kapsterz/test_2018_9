package com.osmolovskyi.test.mailboxes

import akka.actor.ActorSystem
import akka.dispatch.{PriorityGenerator, UnboundedStablePriorityMailbox}
import com.osmolovskyi.test.actors.SlaServiceActor._
import com.typesafe.config.Config

class SlaServiceMailbox(settings: ActorSystem.Settings, config: Config)
  extends UnboundedStablePriorityMailbox(
    PriorityGenerator {

      case _: GetSla => 1

      case _: SlaReceived => 0

    })


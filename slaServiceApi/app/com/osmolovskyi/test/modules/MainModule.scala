package com.osmolovskyi.test.modules

import com.google.inject.AbstractModule
import com.osmolovskyi.test.services._
import javax.inject.Singleton

@Singleton
class MainModule extends AbstractModule {

  override def configure(): Unit = {
    bind(classOf[ThrottlingService]).to(classOf[ThrottlingServiceImpl]).asEagerSingleton()
    bind(classOf[SlaService]).to(classOf[SlaServiceImpl]).asEagerSingleton()
  }

}

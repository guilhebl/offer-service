package common.scheduler

import akka.actor.ActorSystem
import common.config.AppConfigService
import common.email.EmailService
import common.email.model.EmailRequest
import common.executor.RepositoryDispatcherContext
import javax.inject.{Inject, Singleton}
import play.api.Logger
import play.api.inject.ApplicationLifecycle

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

@Singleton
class MongoDbCleanerTask @Inject()(
                           appConfigService: AppConfigService,
                           actorSystem: ActorSystem,
                           lifecycle: ApplicationLifecycle
                         )(implicit executionContext: RepositoryDispatcherContext) {

  private lazy val logger = Logger(this.getClass)
  private lazy val intervalSeconds = appConfigService.properties("scheduler.job.db.cleanup.frequency.seconds").toInt

  actorSystem.scheduler.schedule(initialDelay = intervalSeconds.seconds, interval = intervalSeconds.second) {

    val isEnabled = appConfigService.properties("scheduler.job.db.cleanup.enabled").toBoolean
    logger.info(s"MongoDb Cleanup Job starting... job enabled: $isEnabled")

    if (isEnabled) {

    }
    logger.info(s"MongoDb Cleanup Job completed")
  }

  // This is necessary to avoid thread leaks, specially if you are using a custom ExecutionContext
  lifecycle.addStopHook { () =>
    Future.successful(
      Await.result(actorSystem.terminate(), 240.seconds)
    )
  }

}

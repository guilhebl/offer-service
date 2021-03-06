package common.scheduler

import akka.actor.ActorSystem
import common.config.AppConfigService
import common.executor.RepositoryDispatcherContext
import javax.inject.{Inject, Singleton}
import play.api.Logger
import play.api.inject.ApplicationLifecycle
import product.marketplace.common.MarketplaceRepository

import scala.concurrent.duration._

@Singleton
class MongoDbCleanerTask @Inject()(
                           appConfigService: AppConfigService,
                           marketplaceRepository: MarketplaceRepository,
                           actorSystem: ActorSystem,
                           lifecycle: ApplicationLifecycle
                         )(implicit executionContext: RepositoryDispatcherContext) {

  private lazy val logger = Logger(this.getClass)
  private lazy val intervalSeconds = appConfigService.properties("scheduler.job.db.cleanup.frequency.seconds").toInt

  actorSystem.scheduler.schedule(initialDelay = intervalSeconds.seconds, interval = intervalSeconds.second) {

    val isEnabled = appConfigService.properties("scheduler.job.db.cleanup.enabled").toBoolean
    logger.info(s"MongoDb Cleanup Job starting... job enabled: $isEnabled")

    if (isEnabled) {
      marketplaceRepository.cleanStaleProductTrackings()
    }
    logger.info(s"MongoDb Cleanup Job completed")
  }

}

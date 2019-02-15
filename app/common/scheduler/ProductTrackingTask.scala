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
class ProductTrackingTask @Inject()(
                           marketplaceRepository: MarketplaceRepository,
                           appConfigService: AppConfigService,
                           actorSystem: ActorSystem,
                           lifecycle: ApplicationLifecycle
                         )(
                           implicit executionContext: RepositoryDispatcherContext
                         ) {

  private lazy val logger = Logger(this.getClass)
  private lazy val intervalSeconds = appConfigService.properties("scheduler.job.frequency.seconds").toInt

  actorSystem.scheduler.schedule(initialDelay = intervalSeconds.seconds, interval = intervalSeconds.second) {

    val isEnabled = appConfigService.properties("offer.snapshot.upc.tracker.enabled").toBoolean
    logger.info(s"Product tracking Job starting... enabled: $isEnabled")

    if (isEnabled) {
     // marketplaceRepository.syncTrackedProducts()
    }
    logger.info(s"Product tracking finished")
  }

}

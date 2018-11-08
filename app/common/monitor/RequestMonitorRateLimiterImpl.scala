package common.monitor
import java.util.concurrent.TimeUnit

import com.google.common.util.concurrent.RateLimiter
import common.config.AppConfigService
import javax.inject.{Inject, Singleton}
import play.api.Logger
import product.marketplace.common.MarketplaceConstants.{Amazon, BestBuy, Ebay, Walmart}

import scala.collection.mutable

/**
* An implementation that uses RateLimiter to track numbers of calls per second.
  * @param appConfigService config object
  */
@Singleton
class RequestMonitorRateLimiterImpl @Inject()(appConfigService: AppConfigService) extends RequestMonitor {

  private val logger = Logger(this.getClass)

  val WalmartMaxCapacity = appConfigService.properties("walmartUSMaxCapacity").toInt
  val EbayUSMaxCapacity = appConfigService.properties("ebayUSMaxCapacity").toInt
  val BestbuyUSMaxCapacity = appConfigService.properties("bestbuyUSMaxCapacity").toInt
  val AmazonUSMaxCapacity = appConfigService.properties("amazonUSMaxCapacity").toInt

  val map = mutable.Map[String, RateLimiter](
    Walmart -> RateLimiter.create(WalmartMaxCapacity),
    Ebay -> RateLimiter.create(EbayUSMaxCapacity),
    BestBuy -> RateLimiter.create(BestbuyUSMaxCapacity),
    Amazon -> RateLimiter.create(AmazonUSMaxCapacity)
  )

  override def isRequestPossible(name: String): Boolean = {
    name match {
      case Walmart =>
        isRequestPossible(
          Walmart,
          appConfigService.properties("walmartUSRequestMaxTries").toInt,
          appConfigService.properties("walmartUSRequestWaitInterval").toInt
        )
      case Ebay =>
        isRequestPossible(
          Ebay,
          appConfigService.properties("ebayUSRequestMaxTries").toInt,
          appConfigService.properties("eBayUSRequestWaitInterval").toInt
        )
      case BestBuy =>
        isRequestPossible(
          BestBuy,
          appConfigService.properties("bestbuyUSRequestMaxTries").toInt,
          appConfigService.properties("bestbuyUSRequestWaitInterval").toInt
        )
      case Amazon =>
        isRequestPossible(
          Amazon,
          appConfigService.properties("amazonUSRequestMaxTries").toInt,
          appConfigService.properties("amazonUSRequestWaitInterval").toInt
        )
      case _ => false
    }
  }

  /**
    * Checks if there are available slots to make call
    *
    * @return free or not
    */
  def isTimeSlotFree(name: String, timeout: Long): Boolean = {
    map(name).tryAcquire(timeout, TimeUnit.MILLISECONDS)
  }

  /**
    * Tries to acquire the Request
    * Imperative style is used here due to performance
    * @param name name of entity
    * @param maxTries maxTries of this entity
    * @param timeout how much should a thread wait to try again
    * @return if request is possible after trying 1 <= MaxTries times.
    */
  def isRequestPossible(name: String, maxTries: Int, timeout: Long): Boolean = {
    var tries = 1
    var proceed = isTimeSlotFree(name, timeout)

    while (!proceed && tries < maxTries) {
      logger.debug(s"call to $name API blocked - wait for next period of $timeout ms")
      Thread.sleep(timeout)
      tries += 1
      proceed = isTimeSlotFree(name, timeout)
    }

    if (!proceed) {
      logger.info(s"Unable to acquire lock for $name - Max tries limit reached!")
      false
    } else {
      true
    }
  }

}

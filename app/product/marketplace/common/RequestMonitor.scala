package product.marketplace.common

import java.util.Date

import common.config.AppConfigService
import javax.inject.{Inject, Singleton}
import play.api.Logger
import product.marketplace.common.MarketplaceConstants.{Amazon, BestBuy, Ebay, Walmart}

import scala.collection.mutable.HashMap

trait RequestMonitor {
  def isRequestPossible(name: String): Boolean
}

@Singleton
class RequestMonitorImpl @Inject()(appConfigService: AppConfigService) extends RequestMonitor {

  private val logger = Logger(this.getClass)

  val lastCall: HashMap[String, Long] = HashMap[String, Long](
    Walmart -> 0,
    Ebay -> 0,
    BestBuy -> 0,
    Amazon -> 0
  )

  override def isRequestPossible(name: String): Boolean = {
    name match {
      case Walmart =>
        isRequestPossible(
          Walmart,
          appConfigService.properties("walmartUSRequestMaxTries").toInt,
          appConfigService.properties("walmartUSThreadSleepMillis").toLong,
          appConfigService.properties("walmartUSRequestWaitIntervalMilis").toInt
        )
      case Ebay =>
        isRequestPossible(
          Ebay,
          appConfigService.properties("ebayUSRequestMaxTries").toInt,
          appConfigService.properties("ebayUSThreadSleepMillis").toLong,
          appConfigService.properties("eBayUSRequestWaitIntervalMilis").toInt
        )
      case BestBuy =>
        isRequestPossible(
          BestBuy,
          appConfigService.properties("bestbuyUSRequestMaxTries").toInt,
          appConfigService.properties("bestbuyUSThreadSleepMillis").toLong,
          appConfigService.properties("bestbuyUSRequestWaitIntervalMilis").toInt
        )
      case Amazon =>
        isRequestPossible(
          Amazon,
          appConfigService.properties("amazonUSRequestMaxTries").toInt,
          appConfigService.properties("amazonUSThreadSleepMillis").toLong,
          appConfigService.properties("amazonUSRequestWaitIntervalMilis").toInt
        )
      case _ => false
    }
  }

  /**
    * Tries to detect if a request is possible for this API name
    *
    * @param name api name
    * @param maxTries tries
    * @param waitPeriod wait
    * @param sleepPeriod sleep in milis
    * @return
    */
  def isRequestPossible(name: String, maxTries: Int, waitPeriod: Long, sleepPeriod: Long): Boolean = {
    var tries = 1
    var proceed = isTimeSlotFree(name, waitPeriod)

    while (!proceed && tries < maxTries) {
      logger.debug(s"call to $name API blocked - wait for next period of $waitPeriod ms")
      Thread.sleep(sleepPeriod)
      tries += 1
      proceed = isTimeSlotFree(name, waitPeriod)
    }

    if (tries >= maxTries) {
      logger.info(s"Unable to acquire lock for $name - Max tries limit reached!")
      false
    } else {
      true
    }
  }

  /**
    * Checks if waitIntervalMilis has passed since last Call.
    *
    * Difference in miliseconds (1*1000) = 1 second
    *
    * @return
    */
  def isTimeSlotFree(name: String, waitPeriod: Long): Boolean = synchronized {
    val now = new Date().getTime
    val last = lastCall.getOrElseUpdate(name, 0)
    val diff = now - last

    if (diff >= waitPeriod) {
      lastCall(name) = now
      true
    } else {
      false
    }
  }

}

package app.product.marketplace.common

import java.util.Date

import app.product.marketplace.common.MarketplaceConstants.{Amazon, BestBuy, Ebay, Walmart}
import common.config.AppConfigService
import javax.inject.{Inject, Singleton}
import play.api.Logger

import scala.collection.mutable.HashMap

trait RequestMonitor {
  def isRequestPossible(name : String) : Boolean
}

@Singleton
class RequestMonitorImpl @Inject()(appConfigService: AppConfigService) extends RequestMonitor {
  
  private val logger = Logger(this.getClass)

  val lastCall = HashMap[String, Long](
    Walmart -> 0,
    Ebay -> 0,
    BestBuy -> 0,
    Amazon -> 0
  )

  val waitInterval = Map[String, Int](
    Walmart -> appConfigService.properties("walmartUSRequestWaitIntervalMilis").toInt,
    Ebay -> appConfigService.properties("eBayUSRequestWaitIntervalMilis").toInt,
    BestBuy -> appConfigService.properties("bestbuyUSRequestWaitIntervalMilis").toInt,
    Amazon -> appConfigService.properties("amazonUSRequestWaitIntervalMilis").toInt
  )
  
  override def isRequestPossible(name : String) : Boolean = {
    name match {
      case Walmart => isRequestPossible(Walmart, 
          appConfigService.properties("walmartUSRequestMaxTries").toInt,
          appConfigService.properties("walmartUSThreadSleepMillis").toLong)
      case Ebay => isRequestPossible(Ebay, 
          appConfigService.properties("ebayUSRequestMaxTries").toInt,
          appConfigService.properties("ebayUSThreadSleepMillis").toLong)
      case BestBuy => isRequestPossible(BestBuy, 
          appConfigService.properties("bestbuyUSRequestMaxTries").toInt,
          appConfigService.properties("bestbuyUSThreadSleepMillis").toLong)
      case Amazon => isRequestPossible(Amazon, 
          appConfigService.properties("amazonUSRequestMaxTries").toInt,
          appConfigService.properties("amazonUSThreadSleepMillis").toLong)
      case _ => false
    }  
  }
  
	/**
	 * Checks if waitIntervalMilis has passed since last Call.
	 * 
	 * Difference in miliseconds (1*1000) = 1 second
	 * 
	 * @return
	 */  
  def isTimeSlotFreeWalmart : Boolean = synchronized {
    val now = new Date().getTime
    
		if (now - lastCall(Walmart) >= waitInterval(Walmart)) {
			lastCall(Walmart) = now
			return true
		}
    
    false
  }

  def isTimeSlotFreeBestBuy : Boolean = synchronized {
    val now = new Date().getTime
    
		if (now - lastCall(BestBuy) >= waitInterval(BestBuy)) {
			lastCall(BestBuy) = now
      return true
		}
    
    false
  }

  def isTimeSlotFreeEbay : Boolean = synchronized {
    val now = new Date().getTime
    
		if (now - lastCall(Ebay) >= waitInterval(Ebay)) {
			lastCall(Ebay) = now
      return true
		}
    
    false
  }

  def isTimeSlotFreeAmazon : Boolean = synchronized {
    val now = new Date().getTime
    
		if (now - lastCall(Amazon) >= waitInterval(Amazon)) {
			lastCall(Amazon) = now
      return true
		}
    
    false
  }

  def isTimeSlotFree(name : String) : Boolean = {
    name match {
      case Walmart => isTimeSlotFreeWalmart 
      case Ebay => isTimeSlotFreeEbay
      case BestBuy => isTimeSlotFreeBestBuy
      case Amazon => isTimeSlotFreeAmazon
      case _ => false
    }     
  }

  def isRequestPossible(name : String, maxTries : Int, waitInterval : Long) : Boolean = {
		var tries = 1		
		var proceed = isTimeSlotFree(name)
				
		while (!proceed && tries < maxTries) {						
			logger.trace(s"call to $name API blocked - wait for next period of waitInterval ms")
			Thread.sleep(waitInterval)
			tries += 1
			proceed = isTimeSlotFree(name)			
		}
		
		if (tries >= maxTries) {
		  logger.trace(s"Unable to acquire lock for $name - Max tries limit reached!")
      return false
		}		
		true
  }
  
}
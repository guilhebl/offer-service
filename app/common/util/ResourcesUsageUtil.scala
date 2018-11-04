package common.util

import play.api.Logger

/**
* Utility class to capture the resources usage in a specific point of time
  */
object ResourcesUsageUtil {

  private val logger = Logger(this.getClass)

  val Mb = 1024 * 1024

  def logMemoryUsage(): Unit = {
    val instance = Runtime.getRuntime
    logger.info("***** Heap utilization statistics [MB] *****")

    // available memory
    logger.info("Total Memory: " + instance.totalMemory / Mb)

    // free memory
    logger.info("Free Memory: " + instance.freeMemory / Mb)

    // used memory
    logger.info("Used Memory: " + (instance.totalMemory - instance.freeMemory) / Mb)

    // Maximum available memory
    logger.info("Max Memory: " + instance.maxMemory / Mb)
  }
}

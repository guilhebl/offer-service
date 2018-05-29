package common.log

import java.util.Date

import play.api.Logger

object ThreadLogger {
  private val logger = Logger(this.getClass)

  /**
    * Prints a log message with Thread name and ID.
    *
    * @param msg
    */
  def log(msg : String): Unit = {
    val now = new Date()
    val threadName = Thread.currentThread.getName
    val threadId = Thread.currentThread.getId
    logger.info(s"Thread - $threadId, $threadName: $now - $msg")
  }


}

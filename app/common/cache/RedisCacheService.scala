package common.cache

import com.redis.{RedisClient, Seconds}
import common.config.AppConfigService
import common.executor.WorkerDispatcherContext
import javax.inject.{Inject, Singleton}
import play.api.{Logger, MarkerContext}

/**
  * A pure non-blocking interface
  */
trait RedisCacheService {
  def get(id : String)(implicit mc: MarkerContext): Option[String]
  def set(id: String, doc: String)(implicit mc: MarkerContext): Boolean
  def flushall()(implicit mc: MarkerContext): Boolean
}

@Singleton
class RedisCacheServiceImpl @Inject()
(appConfigService: AppConfigService)(implicit ec: WorkerDispatcherContext)
  extends RedisCacheService {

  private val logger = Logger(this.getClass)
  private val defaultExpiryInSeconds = appConfigService.properties("redis.default.expiry").toLong

  private lazy val redis = new RedisClient(
    appConfigService.properties("redis.host"),
    appConfigService.properties("redis.port").toInt)

  /**
    * Gets a record from Redis Cache
    *
    * @param id
    * @return
    */
  override def get(id : String)(implicit mc: MarkerContext): Option[String] = {
    logger.info(s"Redis get - : $id")
    val item = redis.get(id)
    if (item.isDefined) logger.info("cache hit")
    item
  }

  /**
    * Inserts a record in Redis Cache
    *
    * @return
    */
  override def set(id: String, doc: String)(implicit mc: MarkerContext): Boolean = {
    logger.info(s"Redis set - $id")
    val success = redis.set(id, doc, onlyIfExists = false, Seconds(defaultExpiryInSeconds))
    if (!success) logger.info("cache set FAILURE!")
    success
  }

  /**
    * Resets Redis Cache
    *
    * @return
    */
  override def flushall()(implicit mc: MarkerContext): Boolean = {
    logger.trace(s"Redis flushall")
    redis.flushall
  }


}

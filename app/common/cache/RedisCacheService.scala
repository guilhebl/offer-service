package common.cache

import com.redis.RedisClient
import common.config.AppConfigService
import common.executor.{RepositoryDispatcherContext, WorkerDispatcherContext}
import javax.inject.{Inject, Singleton}
import play.api.{Logger, MarkerContext}

/**
  * A pure non-blocking interface
  */
trait RedisCacheService {
  def get(id : String)(implicit mc: MarkerContext): Option[String]
  def set(id: String, doc: String)(implicit mc: MarkerContext): Boolean
}

@Singleton
class RedisCacheServiceImpl @Inject()
(appConfigService: AppConfigService)(implicit ec: WorkerDispatcherContext)
  extends RedisCacheService {

  private val logger = Logger(this.getClass)
  private val defaultExpiryDate = appConfigService.properties("redis.default.expiry").toLong

  lazy val redis = new RedisClient(
    appConfigService.properties("redis.host"),
    appConfigService.properties("redis.port").toInt)

  /**
    * Gets a record from Redis
    *
    * @param id
    * @return
    */
  override def get(id : String)(implicit mc: MarkerContext): Option[String] = {
    logger.info(s"Redis get - : $id")
    redis.get(id)
  }

  /**
    * Inserts a record in Mongo DB
    *
    * @return
    */
  override def set(id: String, doc: String)(implicit mc: MarkerContext): Boolean = {
    logger.info(s"Redis set - $id, $doc")
    redis.setex(id, defaultExpiryDate, doc)
  }

}

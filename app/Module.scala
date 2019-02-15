import com.google.inject.AbstractModule
import common.cache.{RedisCacheService, RedisCacheServiceImpl}
import common.config.{AppConfigService, AppConfigServiceImpl}
import common.db.{MongoDbService, MongoDbServiceImpl}
import common.email.{EmailService, EmailServiceImpl}
import common.monitor.{RequestMonitor, RequestMonitorImpl}
import common.scheduler.ProductTrackingTask
import geo.{GeolocationRepository, GeolocationRepositoryImpl}
import javax.inject._
import net.codingwell.scalaguice.ScalaModule
import play.api.libs.concurrent.AkkaGuiceSupport
import play.api.{Configuration, Environment}
import product.marketplace.amazon.{AmazonRepository, AmazonRepositoryImpl, AmazonRequestHelper, AmazonRequestHelperImpl}
import product.marketplace.bestbuy.{BestBuyRepository, BestBuyRepositoryImpl}
import product.marketplace.common.{MarketplaceRepository, MarketplaceRepositoryImpl}
import product.marketplace.ebay.{EbayRepository, EbayRepositoryImpl}
import product.marketplace.walmart.{WalmartRepository, WalmartRepositoryImpl}
import product.{ProductRepository, ProductRepositoryImpl}

/**
  * Sets up custom components for Play.
  */
class Module(environment: Environment, configuration: Configuration)
    extends AbstractModule
    with ScalaModule with AkkaGuiceSupport {

  override def configure() = {
    bind[AppConfigService].to[AppConfigServiceImpl].in[Singleton]
    bind[GeolocationRepository].to[GeolocationRepositoryImpl].in[Singleton]
    bind[ProductRepository].to[ProductRepositoryImpl].in[Singleton]
    bind[MarketplaceRepository].to[MarketplaceRepositoryImpl].in[Singleton]
    bind[RequestMonitor].to[RequestMonitorImpl].in[Singleton]
    bind[WalmartRepository].to[WalmartRepositoryImpl].in[Singleton]
    bind[BestBuyRepository].to[BestBuyRepositoryImpl].in[Singleton]
    bind[EbayRepository].to[EbayRepositoryImpl].in[Singleton]
    bind[AmazonRepository].to[AmazonRepositoryImpl].in[Singleton]
    bind[AmazonRequestHelper].to[AmazonRequestHelperImpl].in[Singleton]
    bind[MongoDbService].to[MongoDbServiceImpl].in[Singleton]
    bind[RedisCacheService].to[RedisCacheServiceImpl].in[Singleton]
    bind[EmailService].to[EmailServiceImpl]
    bind(classOf[ProductTrackingTask]).asEagerSingleton()
  }
}
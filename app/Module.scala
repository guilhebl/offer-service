import com.google.inject.AbstractModule
import common.config.{AppConfigService, AppConfigServiceImpl}
import common.db.{MongoRepository, MongoRepositoryImpl}
import geo.{GeolocationRepository, GeolocationRepositoryImpl}
import javax.inject._
import net.codingwell.scalaguice.ScalaModule
import play.api.{Configuration, Environment}
import product.marketplace.amazon.{AmazonRepository, AmazonRepositoryImpl, AmazonRequestHelper, AmazonRequestHelperImpl}
import product.marketplace.bestbuy.{BestBuyRepository, BestBuyRepositoryImpl}
import product.marketplace.common.{MarketplaceRepository, MarketplaceRepositoryImpl, RequestMonitor, RequestMonitorImpl}
import product.marketplace.ebay.{EbayRepository, EbayRepositoryImpl}
import product.marketplace.walmart.{WalmartRepository, WalmartRepositoryImpl}
import product.{ProductRepository, ProductRepositoryImpl}

/**
  * Sets up custom components for Play.
  */
class Module(environment: Environment, configuration: Configuration)
    extends AbstractModule
    with ScalaModule {

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
    bind[MongoRepository].to[MongoRepositoryImpl].in[Singleton]
  }
}

package product.marketplace.common

import product.model.{OfferDetail, OfferList}

import scala.concurrent.Future

trait MarketplaceProviderRepository {
  def search(params: Map[String,String]) : Future[Option[OfferList]]
  def getProductDetail(id: String, idType : String, country : Option[String]) : Future[Option[OfferDetail]]
}

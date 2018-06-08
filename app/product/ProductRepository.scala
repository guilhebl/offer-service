package product

import play.api.MarkerContext
import product.model.{ListRequest, OfferDetail, OfferList}

import scala.concurrent.Future

/**
  * A pure non-blocking interface for the ProductRepository.
  */
trait ProductRepository {
  def search(listRequest : ListRequest)(implicit mc: MarkerContext): Future[Option[OfferList]]
  def get(id : String, idType : String, source : String, country : Option[String])(implicit mc: MarkerContext): Future[Option[OfferDetail]]
}

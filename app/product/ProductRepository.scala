package product

import app.product.model.{ListRequest, OfferDetail, OfferList}
import play.api.MarkerContext

import scala.concurrent.Future

/**
  * A pure non-blocking interface for the ProductRepository.
  */
trait ProductRepository {
  def search(listRequest : ListRequest)(implicit mc: MarkerContext): Future[Option[OfferList]]
  def get(id : String, idType : String, source : String, country : Option[String])(implicit mc: MarkerContext): Future[Option[OfferDetail]]
}

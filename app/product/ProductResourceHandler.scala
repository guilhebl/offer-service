package product

import javax.inject.Inject
import play.api.MarkerContext
import product.model._

import scala.concurrent.{ExecutionContext, Future}


/**
  * Controls access to the backend data
  */
class ProductResourceHandler @Inject()(productRepository: ProductRepository)(implicit ec: ExecutionContext) {

  def get(id : String, idType : String, source : String)(implicit mc: MarkerContext): Future[Option[OfferDetail]] = {
    productRepository.get(id, idType, source)
  }

  def search(listRequest : ListRequest)(implicit mc: MarkerContext): Future[Option[OfferList]] = {
    productRepository.search(listRequest)
  }

}


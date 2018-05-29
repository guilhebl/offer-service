package product

import app.product.marketplace.common.MarketplaceRepository
import app.product.model.{ListRequest, OfferDetail, OfferList}
import common.executor.RepositoryDispatcherContext
import javax.inject.{Inject, Singleton}
import play.api.MarkerContext

import scala.concurrent.Future

/**
  * A trivial implementation for the Product Repository.
  *
  * A custom execution context is used here to establish that blocking operations should be
  * executed in a different thread than Play's ExecutionContext, which is used for CPU bound tasks
  * such as rendering.
  *
  */
@Singleton
class ProductRepositoryImpl @Inject()(marketplaceRepository: MarketplaceRepository)(implicit ec: RepositoryDispatcherContext)
  extends ProductRepository {

  override def search(listRequest : ListRequest)(implicit mc: MarkerContext): Future[Option[OfferList]] = {
    marketplaceRepository.search(listRequest)
  }

  override def get(id : String, idType : String, source : String, country : Option[String])(implicit mc: MarkerContext): Future[Option[OfferDetail]] = {
    marketplaceRepository.getProductDetail(id, idType, source, country)
  }

}

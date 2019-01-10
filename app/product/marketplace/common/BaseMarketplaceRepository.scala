package product.marketplace.common

import common.executor.model.BaseDomainRepository
import common.log.ThreadLogger
import product.model.{ListRequest, OfferDetail, OfferList}

import scala.annotation.tailrec
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.language.postfixOps

/**
  * A pure non-blocking interface
  */
trait BaseMarketplaceRepository extends BaseDomainRepository {
  def search(req: ListRequest): Future[Option[OfferList]]
  def searchAll(req: ListRequest): Future[Option[OfferList]]
  def getProductDetail(id: String, idType: String, source: String): Future[Option[OfferDetail]]

  /**
    * Base algorithm for recursive searching
    *
    * @param acc accumulator
    * @param request params
    * @param page current page
    * @param timeout timeoutUsed that will be used to wait for this call
    * @return result of single iteration or final result
    */
  protected def searchAll(acc: OfferList, request: ListRequest, timeout: Int): Future[Option[OfferList]] = {
    @tailrec
    def searchOffers(acc: OfferList, request: ListRequest, page: Int, timeout: Int): Future[Option[OfferList]] = {
      ThreadLogger.log(s"searchAll - request: $request, $page")
      val future = search(ListRequest.fromPage(request, page))
      val result = Await.result(future, timeout millis)
      val merged = OfferList.merge(Some(acc), result)

      // verify if above last page
      if (page + 1 > merged.get.summary.pageCount) {
        Future.successful(merged)
      } else {
        searchOffers(merged.get, request, page + 1, timeout)
      }
    }

    searchOffers(acc, request, 1, timeout)
  }

}

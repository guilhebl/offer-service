package product.marketplace.bestbuy

import common.config.AppConfigService
import common.executor.WorkerDispatcherContext
import common.log.ThreadLogger
import javax.inject.{Inject, Singleton}
import play.api.Logger
import play.api.libs.json._
import play.api.libs.ws._
import product.marketplace.bestbuy.model._
import product.marketplace.common.MarketplaceConstants._
import product.marketplace.common.{MarketplaceProviderRepository, RequestMonitor}
import product.model._

import scala.collection.mutable.ListBuffer
import scala.concurrent.Future
import scala.concurrent.duration._

trait BestBuyRepository extends MarketplaceProviderRepository

@Singleton
class BestBuyRepositoryImpl @Inject()(ws: WSClient, appConfigService: AppConfigService, requestMonitor: RequestMonitor)
                                     (implicit ec: WorkerDispatcherContext) extends BestBuyRepository {

  private val logger = Logger(this.getClass)

  override def search(params: Map[String, String]): Future[Option[OfferList]] = {
    ThreadLogger.log("BestBuy Search")
    val p = filterParamsSearch(params)

    // try to acquire lock from request Monitor
    if (!requestMonitor.isRequestPossible(BestBuy)) {
      logger.info(s"Unable to acquire lock from Request Monitor")
      return Future.successful(None)
    }

    val endpoint: String = appConfigService.properties("bestbuyUSEndpoint")
    val keywordSearch = p.contains(Keywords)
    val page = p(Page).toInt
    val pageSize = appConfigService.properties("bestbuyUSDefaultPageSize").toInt
    val apiKey = appConfigService.properties("bestbuyUSapiKey")
    val affiliateId = appConfigService.properties("bestbuyUSLinkShareId")
    val timeout = appConfigService.properties("marketplaceDefaultTimeout")

    if (keywordSearch) {
      val listFields = appConfigService.properties("bestbuyUSListFields")
      val path = appConfigService.properties("bestbuyUSProductSearchPath")
      val keywords = p(Keywords)
      val url = s"$endpoint/$path$keywords"

      val req: WSRequest = ws
        .url(url)
        .addHttpHeaders("Accept" -> "application/json")
        .addQueryStringParameters(
          "format" -> "json",
          "apiKey" -> apiKey,
          "LID" -> affiliateId,
          "show" -> listFields,
          "page" -> page.toString,
          "pageSize" -> pageSize.toString
        )
        .withRequestTimeout(timeout.toInt.millis)

      logger.info("BestBuy: " + req.uri)

      val futureResult: Future[Option[BestBuySearchResponse]] = req
        .get()
        .map { response => {
            val resp = response.json.validate[BestBuySearchResponse]
            resp match {
              case s: JsSuccess[BestBuySearchResponse] => Some(s.get)
              case e: JsError =>
                logger.info("Errors: " + JsError.toJson(e).toString())
                None
            }
          }
        }

      futureResult.map {
       case Some(entity) => buildList(entity, pageSize)
       case _ => None
      }

    } else {
      // picks up trending products if no search query present
      val path = appConfigService.properties("bestbuyUSProductTrendingPath")
      val url = s"$endpoint/$path"
      val req: WSRequest = ws
        .url(url)
        .addHttpHeaders("Accept" -> "application/json")
        .addQueryStringParameters("format" -> "json", "apiKey" -> apiKey, "LID" -> affiliateId)
        .withRequestTimeout(timeout.toInt.millis)

      logger.info("BestBuy Trending: " + req.uri)

      val futureResult: Future[Option[BestBuyTrendingResponse]] = req
        .get()
        .map { response => {
            val resp = response.json.validate[BestBuyTrendingResponse]
            resp match {
              case s: JsSuccess[BestBuyTrendingResponse] => Some(s.get)
              case e: JsError =>
                logger.info("Errors: " + JsError.toJson(e).toString())
                None
            }
          }
        }

      futureResult.map {
        case Some(entity) => buildList(entity)
        case _ => None
      }
    }
  }

  private def filterIdType(str: String): Option[String] = {
    str match {
      case Id => Some("productId")
      case Upc => Some("upc")
      case _ => None
    }
  }

  override def getProductDetail(id: String, idType: String, country: Option[String]): Future[Option[OfferDetail]] = {
    ThreadLogger.log("BestBuy getProductDetail")
    val idTypeBestBuy = filterIdType(idType)

    if (idTypeBestBuy.isEmpty) return Future.successful(None)

    // try to acquire lock from request Monitor
    if (!requestMonitor.isRequestPossible(BestBuy)) {
      logger.info(s"Unable to acquire lock from Request Monitor")
      return Future.successful(None)
    }

    val endpoint: String = appConfigService.properties("bestbuyUSEndpoint")
    val path = appConfigService.properties("bestbuyUSProductSearchPath")
    val apiKey = appConfigService.properties("bestbuyUSapiKey")
    val affiliateId = appConfigService.properties("bestbuyUSLinkShareId")
    val timeout = appConfigService.properties("marketplaceDefaultTimeout")
    val idTypeStr = idTypeBestBuy.get

    val url = s"$endpoint/$path($idTypeStr=$id)"

    val req: WSRequest = ws
      .url(url)
      .addHttpHeaders("Accept" -> "application/json")
      .addQueryStringParameters("format" -> "json", "apiKey" -> apiKey, "LID" -> affiliateId)
      .withRequestTimeout(timeout.toInt.millis)

    logger.info("BestBuy: " + req.uri)

    val futureResult: Future[Option[BestBuySearchResponse]] = req
      .get()
      .map { response => {
          val resp = response.json.validate[BestBuySearchResponse]
          resp match {
            case s: JsSuccess[BestBuySearchResponse] => Some(s.get)
            case e: JsError =>
              logger.info("Errors: " + JsError.toJson(e).toString())
              None
          }
        }
      }

    futureResult.map {
      case Some(entity) => buildProductDetail(entity)
      case _ => None
    }
  }

  private def buildProductDetail(item: ProductItem): Option[OfferDetail] = {
    ThreadLogger.log("BestBuy build ProductDetail")
    val proxyRequired = appConfigService.properties("marketplaceProvidersImageProxyRequired").indexOf(BestBuy) != -1

    val detail = new OfferDetail(
      new Offer(
        item.productId.get.toString,
        item.upc,
        item.name,
        BestBuy,
        item.url.get,
        appConfigService.buildImgUrlExternal(item.image, proxyRequired),
        appConfigService.buildImgUrl(Some("best-buy-logo.png")),
        item.salePrice,
        buildCategoryPath(item.categoryPath),
        item.customerReviewAverage.getOrElse(0.0f),
        item.customerReviewCount.getOrElse(0)
      ),
      "",
      buildProductDetailAttributes(item.manufacturer),
      ListBuffer.empty[OfferDetailItem]
    )

    Some(detail)
  }

  private def buildProductDetailAttributes(str: Option[String]): ListBuffer[NameValue] = {
    str match {
      case Some(s) =>
        val listBuffer = ListBuffer.empty[NameValue]
        listBuffer += new NameValue("manufacturer", s)
        listBuffer
      case _ => ListBuffer.empty[NameValue]
    }
  }

  private def buildProductDetail(item: BestBuySearchResponse): Option[OfferDetail] = {
    if (item.products.isEmpty) return None
    buildProductDetail(item.products.head)
  }

  /**
	 * Builds search path pattern for US best buy api
	 *
	 * sample: input 'deals of the day' : output -> (search=deals&search=of&search=the&search=day)
	 *
	 * @param s
	 * @return
	 */
  private def buildSearchPath(s: String): String = {
    val sb: StringBuilder = new StringBuilder("")
    sb.append('(')
    val keywords = s.split(" ")
    for (k <- keywords) {
      if (k != null && !k.equals("")) sb.append("search=" + k + "&")
    }

    sb.deleteCharAt(sb.length() - 1) // remove last &
    sb.append(')')

    sb.toString()
  }

  private def filterParamsSearch(params: Map[String, String]): Map[String, String] = {
    val p = scala.collection.mutable.Map[String,String]()

    // get search keyword phrase
    if (params.contains(Name)) p += ("keywords" -> buildSearchPath(params(Name)))

    // get page - defaults to 1
    params.get(Page) match {
      case None => p(Page) = 1.toString
      case Some(v) => p(Page) = v
    }

    p.toMap
  }

  private def buildList(r: BestBuySearchResponse, pageSize: Int): Option[OfferList] = {
    ThreadLogger.log("BestBuy buildList")

    if (r.products.isEmpty) return None
    val summary = new ListSummary(r.currentPage, r.totalPages, r.total)
    val resp = new OfferList(buildListItems(r.products), summary)
    Some(resp)
  }

  private def buildList(r: BestBuyTrendingResponse): Option[OfferList] = {
    ThreadLogger.log("BestBuy build Trending")

    if (r.results.isEmpty) return None
    val summary = new ListSummary(1, 1, r.metadata.resultSet.count)
    val resp = new OfferList(buildTrendingListItems(r.results), summary)
    Some(resp)
  }

  private def buildTrendingListItems(items: Iterable[ProductSpecialOfferItem]): Iterable[Offer] = {
    val proxyRequired = appConfigService.properties("marketplaceProvidersImageProxyRequired").indexOf(BestBuy) != -1

    val list = items.map((item: ProductSpecialOfferItem) => {
      new Offer(
        item.sku,
        None,
        item.names.title,
        BestBuy,
        item.links.web,
        appConfigService.buildImgUrlExternal(Some(item.images.standard), proxyRequired),
        appConfigService.buildImgUrl(Some("best-buy-logo.png")),
        item.prices.current,
        "special offer",
        item.customerReviews.averageScore.getOrElse(0.0f),
        item.customerReviews.count.getOrElse(0)
      )
    })

    list
  }

  private def buildListItems(items: Iterable[ProductItem]): Iterable[Offer] = {
    val proxyRequired = appConfigService.properties("marketplaceProvidersImageProxyRequired").indexOf(BestBuy) != -1

    val list = items.map(
      item =>
        new Offer(
          if (item.productId.isDefined) item.productId.get.toString else "",
          if (item.upc.isDefined) Some(item.upc.get.toString) else None,
          item.name,
          BestBuy,
          item.url.getOrElse(""),
          appConfigService.buildImgUrlExternal(item.image, proxyRequired),
          appConfigService.buildImgUrl(Some("best-buy-logo.png")),
          item.salePrice,
          buildCategoryPath(item.categoryPath),
          item.customerReviewAverage.getOrElse(0.0f),
          item.customerReviewCount.getOrElse(0)
      )
    )
    list
  }

  private def buildCategoryPath(path: Iterable[CategoryPath]) = {
    val sb: StringBuilder = new StringBuilder("")
    for (p <- path) {
      sb.append(p.name + "-")
    }
    sb.deleteCharAt(sb.length() - 1) // remove last -
    sb.toString()
  }

}

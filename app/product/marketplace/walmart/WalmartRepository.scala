package product.marketplace.walmart

import common.config.AppConfigService
import common.executor.WorkerDispatcherContext
import common.log.ThreadLogger
import common.util.RegexUtil._
import javax.inject.{Inject, Singleton}
import play.api.Logger
import play.api.libs.json._
import play.api.libs.ws._
import product.marketplace.common.MarketplaceConstants._
import product.marketplace.common.{MarketplaceProviderRepository, RequestMonitor}
import product.marketplace.walmart.model.{WalmartSearchBaseResponse, WalmartSearchItem, WalmartSearchResponse, WalmartTrendingSearchResponse}
import product.model._

import scala.collection.mutable.HashMap
import scala.concurrent.Future
import scala.concurrent.duration._

trait WalmartRepository extends MarketplaceProviderRepository

@Singleton
class WalmartRepositoryImpl @Inject()
(ws: WSClient, appConfigService: AppConfigService, requestMonitor: RequestMonitor)
(implicit ec: WorkerDispatcherContext) extends WalmartRepository {

  private val logger = Logger(this.getClass)

  override def search(params: Map[String, String]): Future[Option[OfferList]] = {
    ThreadLogger.log("Walmart search")

    val p = filterParamsSearch(params)
    
    // try to acquire lock from request Monitor
		if (!requestMonitor.isRequestPossible(Walmart)) {
		    logger.info(s"Unable to acquire lock from Request Monitor")
        return Future.successful(None)
		}
    
    val endpoint: String = appConfigService.properties("walmartUSEndpoint")
    val keywordSearch = p.contains("query")
    val page = p(Page).toInt
    val pageSize = appConfigService.properties("walmartUSDefaultPageSize").toInt
    val start = if (page > 1) ((page - 1) * pageSize) + 1 else 1
    val responseGroup = appConfigService.properties("walmartUSsearchResponseGroup")
    val apiKey = appConfigService.properties("walmartUSapiKey")
    val affiliateId = appConfigService.properties("walmartUSAffiliateId")
    val timeout = appConfigService.properties("marketplaceDefaultTimeout")

    if (keywordSearch) {
      val url = endpoint + '/' + appConfigService.properties("walmartUSProductSearchPath")
      val req: WSRequest = ws.url(url)
        .addHttpHeaders("Accept" -> "application/json")
        .addQueryStringParameters(
            "format" -> "json",
            "responseGroup" -> responseGroup,
            "apiKey" -> apiKey,
            "lsPublisherId" -> affiliateId,
            "query" -> p("query"),
            "start" -> start.toString)
        .withRequestTimeout(timeout.toInt.millis)

      logger.info("Walmart: " + req.uri)

      val futureResult: Future[Option[WalmartSearchResponse]] = req.get().map {
          response =>
            {
              val resp = response.json.validate[WalmartSearchResponse]
              resp match {
                case s: JsSuccess[WalmartSearchResponse] => Some(s.get)
                case e: JsError => {
                  logger.info("Errors: " + JsError.toJson(e).toString())
                  None
                }
              }
            }
      }

      futureResult.map(r => {
        r match {
          case Some(entity) => buildList(entity, pageSize)
          case _ => None
        }
      })

    } else {
      // picks up trending products if no search query present      
      val url = endpoint + '/' + appConfigService.properties("walmartUSProductTrendingPath")
      val req: WSRequest = ws.url(url)
        .addHttpHeaders("Accept" -> "application/json")
        .addQueryStringParameters(
          "format" -> "json",
          "responseGroup" -> responseGroup,
          "apiKey" -> apiKey,
          "lsPublisherId" -> affiliateId)
        .withRequestTimeout(timeout.toInt.millis)

      logger.info("Walmart trending: " + req.uri)

      val futureResult: Future[Option[WalmartTrendingSearchResponse]] = req.get().map {
          response =>
            {
              val resp = (response.json).validate[WalmartTrendingSearchResponse]
              resp match {
                case s: JsSuccess[WalmartTrendingSearchResponse] => Some(s.get)
                case e: JsError => {
                  logger.info("Errors: " + JsError.toJson(e).toString())
                  None
                }
              }
            }
      }

      futureResult.map(r => {
        r match {
          case Some(entity) => buildList(entity, page, pageSize)
          case _ => None
        }
      })

    }
  }

  override def getProductDetail(id: String, idType : String, country : Option[String]): Future[Option[OfferDetail]] = {
      ThreadLogger.log("Walmart get product Detail")

      // try to acquire lock from request Monitor
      if (!requestMonitor.isRequestPossible(Walmart)) {
        logger.info(s"Unable to acquire lock from Request Monitor")
        return Future.successful(None)
      }

      val endpoint: String = appConfigService.properties("walmartUSEndpoint")
      val path: String = appConfigService.properties("walmartUSProductDetailPath")
      val apiKey = appConfigService.properties("walmartUSapiKey")
      val affiliateId = appConfigService.properties("walmartUSAffiliateId")
      val timeout = appConfigService.properties("marketplaceDefaultTimeout")

      idType match {
        case Id => {
          val url = endpoint + '/' + path + '/' + id
          val req: WSRequest = ws.url(url)
            .addHttpHeaders("Accept" -> "application/json")
            .addQueryStringParameters(
              "format" -> "json",
              "apiKey" -> apiKey,
              "lsPublisherId" -> affiliateId)
            .withRequestTimeout(timeout.toInt.millis)
          logger.info("Walmart get: " + req.uri)
          val futureResult: Future[Option[WalmartSearchItem]] = req.get().map {
            response =>
            {
              val resp = response.json.validate[WalmartSearchItem]
              resp match {
                case s: JsSuccess[WalmartSearchItem] => Some(s.get)
                case e: JsError => {
                  logger.info("Errors: " + JsError.toJson(e).toString())
                  None
                }
              }
            }
          }
          futureResult.map(r => {
            r match {
              case Some(entity) => buildProductDetail(entity)
              case _ => None
            }
          })
        }
        case Upc => {
          val url = endpoint + '/' + path
          val req: WSRequest = ws.url(url)
            .addHttpHeaders("Accept" -> "application/json")
            .addQueryStringParameters(
              Upc -> id,
              "format" -> "json",
              "apiKey" -> apiKey,
              "lsPublisherId" -> affiliateId)
            .withRequestTimeout(timeout.toInt.millis)
          logger.info("Walmart get by Upc: " + req.uri)
          val futureResult: Future[Option[WalmartSearchBaseResponse]] = req.get().map {
            response =>
            {
              val resp = response.json.validate[WalmartSearchBaseResponse]
              resp match {
                case s: JsSuccess[WalmartSearchBaseResponse] => Some(s.get)
                case e: JsError => {
                  logger.info("Errors: " + JsError.toJson(e).toString())
                  None
                }
              }
            }
          }
          futureResult.map(r => {
            r match {
              case Some(entity) => buildProductDetail(entity)
              case _ => None
            }
          })
        }

        case _ => Future.successful(None)
      }

  }

  private def filterParamsSearch(params: Map[String, String]): Map[String, String] = {
    val p: HashMap[String, String] = HashMap()

    // get search keyword phrase
    if (params.contains(Name)) p("query") = params(Name)

    // get page - defaults to 1
    p(Page) = params.getOrElse(Page, "1")

    p.toMap
  }

  private def buildList(r: WalmartSearchResponse, pageSize: Int): Option[OfferList] = {
    ThreadLogger.log("Walmart buildList")
    val summary = new ListSummary(r.start / pageSize + 1, r.totalResults / pageSize, r.totalResults)
    val resp = new OfferList(buildListItems(r.items), summary)
    Some(resp)
  }

  private def buildList(r: WalmartTrendingSearchResponse, currPage: Int, pageSize: Int): Option[OfferList] = {
    ThreadLogger.log("Walmart build Trending")
    val summary = new ListSummary(currPage, r.items.size / pageSize, r.items.size)
    val resp = new OfferList(buildListItems(r.items), summary)
    Some(resp)
  }

  private def buildProductDetail(item : WalmartSearchItem): Option[OfferDetail] = {
    ThreadLogger.log("Walmart build product Detail")
    val proxyRequired = appConfigService.properties("marketplaceProvidersImageProxyRequired").indexOf(Walmart) != -1

    val offerDetail = new OfferDetail(
      new Offer(
        item.itemId.toString,
        item.upc,
        item.name,
        Walmart,
        item.productTrackingUrl,
        appConfigService.buildImgUrlExternal(Some(item.largeImage.get), proxyRequired),
        appConfigService.buildImgUrl(Some("walmart-logo.png")),
        item.salePrice.getOrElse(0.1d),
        item.categoryPath,
        item.customerRating.getOrElse("0").toFloat,
        item.numReviews.getOrElse(0)),
      filterHtmlTags(item.longDescription),
      List[NameValue](),
      List[OfferDetailItem]())

    Some(offerDetail)
  }

  private def buildProductDetail(item : WalmartSearchBaseResponse): Option[OfferDetail] = {
    buildProductDetail(item.items.head)
  }

  private def buildListItems(items: Iterable[WalmartSearchItem]): Iterable[Offer] = {
    val proxyRequired = appConfigService.properties("marketplaceProvidersImageProxyRequired").indexOf(Walmart) != -1

    val list = items.map((item: WalmartSearchItem) => {
      new Offer(
        item.itemId.toString,
        item.upc,
        item.name,
        Walmart,
        item.productTrackingUrl,
        appConfigService.buildImgUrlExternal(Some(item.largeImage.get), proxyRequired),
        appConfigService.buildImgUrl(Some("walmart-logo.png")),
        item.salePrice.getOrElse(0.1d),
        item.categoryPath,
        item.customerRating.getOrElse("0").toFloat,
        item.numReviews.getOrElse(0))
    })
    list
  }

}

package app.product.marketplace.ebay

import app.product.marketplace.common.MarketplaceConstants._
import app.product.marketplace.common.{MarketplaceProviderRepository, RequestMonitor}
import app.product.marketplace.ebay.model.{EbayProductDetailResponse, EbaySearchResponse, SearchResultItem}
import app.product.model._
import common.config.AppConfigService
import common.executor.WorkerDispatcherContext
import common.log.ThreadLogger
import javax.inject.{Inject, Singleton}
import play.api.Logger
import play.api.libs.json._
import play.api.libs.ws._

import scala.collection.mutable.HashMap
import scala.concurrent.Future
import scala.concurrent.duration._

trait EbayRepository extends MarketplaceProviderRepository

@Singleton
class EbayRepositoryImpl @Inject()(ws: WSClient, appConfigService: AppConfigService,requestMonitor: RequestMonitor)
(implicit ec: WorkerDispatcherContext) extends EbayRepository {

  private val logger = Logger(this.getClass)

  override def search(params: Map[String, String]): Future[Option[OfferList]] = {
    ThreadLogger.log("Ebay Search")

    val p = filterParamsSearch(params)
    
    // try to acquire lock from request Monitor
		if (!requestMonitor.isRequestPossible(Ebay)) {
		    logger.info(s"Unable to acquire lock from Request Monitor")
        return Future.successful(None)
		}

    val endpoint: String = appConfigService.properties("eBayEndpoint")
    val timeout = appConfigService.properties("marketplaceDefaultTimeout")
    val path = appConfigService.properties("eBayProductSearchPath")
    val pageSize = appConfigService.properties("eBayDefaultPageSize")
    val ebaySecurityAppName = appConfigService.properties("eBaySecurityAppName")
    val ebayDefaultDataFormat = appConfigService.properties("eBayDefaultDataFormat")
    val ebayAffiliateNetworkId = appConfigService.properties("eBayAffiliateNetworkId")
    val ebayAffiliateTrackingId = appConfigService.properties("eBayAffiliateTrackingId")
    val ebayAffiliateCustomId = appConfigService.properties("eBayAffiliateCustomId")
    
    val url = endpoint + '/' + path

    val req: WSRequest = ws.url(url)
        .addHttpHeaders("Accept" -> "application/json")
        .addQueryStringParameters(
            "OPERATION-NAME" -> "findItemsByKeywords",
            "SERVICE-VERSION" -> "1.0.0",
            "SECURITY-APPNAME" -> ebaySecurityAppName,
            "GLOBAL-ID" -> getEbayGlobalId(p(Country)),
            "RESPONSE-DATA-FORMAT" -> ebayDefaultDataFormat,
            "affiliate.networkId" -> ebayAffiliateNetworkId,
            "affiliate.trackingId" -> ebayAffiliateTrackingId,
            "affiliate.customId" -> ebayAffiliateCustomId,
            "outputSelector" -> "PictureURLLarge", // add large picture to standard result            
            "paginationInput.pageNumber" -> p(Page),
            "paginationInput.entriesPerPage" -> pageSize.toString,
            Keywords -> p(Keywords))
        .withRequestTimeout(timeout.toInt.millis)

    logger.info("Ebay: " + req.uri)

    val futureResult: Future[Option[EbaySearchResponse]] = req.get()
        .map {
          response => {
            val resp = (response.json).validate[EbaySearchResponse]
            resp match {
              case s: JsSuccess[EbaySearchResponse] => Some(s.get)
              case e: JsError => {
                logger.info("Errors: " + JsError.toJson(e).toString())
                None
              }
            }
          }
        }

    futureResult.map(r => {
      r match {
        case Some(entity) => buildList(entity)
        case _ => None
      }
    })
  }
  
  private def getEbayGlobalId(country : String) = {
    country match {
      case Canada => "EBAY-ENCA"
      case _ => "EBAY-US"
    }
  }

  override def getProductDetail(id: String, idType : String, country : Option[String]): Future[Option[OfferDetail]] = {
    // try to acquire lock from request Monitor
    if (!requestMonitor.isRequestPossible(Ebay)) {
      logger.info(s"Unable to acquire lock from Request Monitor")
      return Future.successful(None)
    }

    val endpoint: String = appConfigService.properties("eBayEndpoint")
    val timeout = appConfigService.properties("marketplaceDefaultTimeout")
    val path = appConfigService.properties("eBayProductSearchPath")
    val ebaySecurityAppName = appConfigService.properties("eBaySecurityAppName")
    val ebayDefaultDataFormat = appConfigService.properties("eBayDefaultDataFormat")
    val ebayAffiliateNetworkId = appConfigService.properties("eBayAffiliateNetworkId")
    val ebayAffiliateTrackingId = appConfigService.properties("eBayAffiliateTrackingId")
    val ebayAffiliateCustomId = appConfigService.properties("eBayAffiliateCustomId")

    val url = endpoint + '/' + path

    idType match {
      case Id | Upc => {

        val idTypeEbay = getEbayIdType(idType)

        // if idType is invalid return empty response
        if (!idTypeEbay.isDefined) return Future.successful(None)

        val req: WSRequest = ws.url(url)
          .addHttpHeaders("Accept" -> "application/json")
          .addQueryStringParameters(
            "OPERATION-NAME" -> "findItemsByProduct",
            "SERVICE-VERSION" -> "1.0.0",
            "SECURITY-APPNAME" -> ebaySecurityAppName,
            "GLOBAL-ID" -> getEbayGlobalId(country.get),
            "RESPONSE-DATA-FORMAT" -> ebayDefaultDataFormat,
            "affiliate.networkId" -> ebayAffiliateNetworkId,
            "affiliate.trackingId" -> ebayAffiliateTrackingId,
            "affiliate.customId" -> ebayAffiliateCustomId,
            "outputSelector" -> "PictureURLLarge", // add large picture to standard result
            "productId.@type" -> idTypeEbay.get,
            "productId" -> id,
            "paginationInput.entriesPerPage" -> "1"
          ).withRequestTimeout(timeout.toInt.millis)

        logger.info("Ebay get By " + idTypeEbay + " " + req.uri)

        val futureResult: Future[Option[EbayProductDetailResponse]] = req.get()
          .map {
            response => {
              val resp = (response.json).validate[EbayProductDetailResponse]
              resp match {
                case s: JsSuccess[EbayProductDetailResponse] => Some(s.get)
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

  private def getEbayIdType(idType : String): Option[String] = {
    idType match {
      case Id => Some("ReferenceID")
      case Upc => Some("UPC")
      case _ => None
    }
  }
  
  private def filterParamsSearch(params: Map[String, String]): Map[String, String] = {
    val p: HashMap[String, String] = HashMap()

    // get search keyword phrase
    if (params.contains(Name)) {
      p(Keywords) = params(Name)
    } else {
      p(Keywords) = getRandomSearchQuery(appConfigService.properties("eBayDefaultSearchQuery"), ",")
    }

    // get page - defaults to 1
    params.get(Page) match {
      case None    => p(Page) = 1.toString
      case Some(v) => p(Page) = v
    }

    // get Country
    if (params.contains(Country)) p(Country) = params(Country) else p(Country) = UnitedStates

    p.toMap
  }
  
  private def getRandomSearchQuery(query: String, separator: String) = {
    val strings = query.split(separator)    
    val r = scala.util.Random
    strings(r.nextInt(strings.length))
  }
  
  private def buildList(r: EbaySearchResponse): Option[OfferList] = {
    ThreadLogger.log("Ebay buildList")
    val summary = new ListSummary(
        r.findItemsByKeywordsResponse.head.paginationOutput.get.head.pageNumber.head.toInt,
        r.findItemsByKeywordsResponse.head.paginationOutput.get.head.totalPages.head.toInt,
        r.findItemsByKeywordsResponse.head.paginationOutput.get.head.totalEntries.head.toInt)
    val resp = new OfferList(buildListItems(r.findItemsByKeywordsResponse.head.searchResult.get.head.item), summary)
    Some(resp)
  }

  private def buildListItems(items: Iterable[SearchResultItem]): Iterable[Offer] = {
    val proxyRequired = appConfigService.properties("marketplaceProvidersImageProxyRequired").indexOf(Ebay) != -1
    
    val list = items.map((item: SearchResultItem) => {
      val productId = if (!item.productId.isDefined) item.itemId.head else item.productId.get.head.value
      val imgUrl = if (!item.pictureURLLarge.isDefined) "" else item.pictureURLLarge.get.head
      val itemSellerUrl = if (item.viewItemURL == null) "" else item.viewItemURL.head

      new Offer(
        productId,
        None,
        item.title.mkString,
        Ebay,
        itemSellerUrl,
        appConfigService.buildImgUrlExternal(Some(imgUrl), proxyRequired),
        appConfigService.buildImgUrl(Some("ebay-logo.png")),
        item.sellingStatus.head.convertedCurrentPrice.head.value.toDouble,
        item.primaryCategory.head.categoryName.mkString,
        0,
        0)
    })
    list
  }

    private def buildProductDetail(item : SearchResultItem): Option[OfferDetail] = {
      val proxyRequired = appConfigService.properties("marketplaceProvidersImageProxyRequired").indexOf(Ebay) != -1

      val productId = if (!item.productId.isDefined) item.itemId.head else item.productId.get.head.value
      val imgUrl = if (!item.pictureURLLarge.isDefined) "" else item.pictureURLLarge.get.head
      val itemSellerUrl = if (item.viewItemURL == null) "" else item.viewItemURL.head

      val detail = new OfferDetail(
        new Offer(
        productId,
        None,
        item.title.mkString,
        Ebay,
        itemSellerUrl,
        appConfigService.buildImgUrlExternal(Some(imgUrl), proxyRequired),
        appConfigService.buildImgUrl(Some("ebay-logo.png")),
        item.sellingStatus.head.convertedCurrentPrice.head.value.toDouble,
        item.primaryCategory.head.categoryName.mkString,
        0,
        0),
        "",
        List[NameValue](),
        List[OfferDetailItem]())

      Some(detail)
    }

    private def buildProductDetail(item : EbayProductDetailResponse): Option[OfferDetail] = {
      if (!item.findItemsByProductResponse.head.searchResult.isDefined) return None
      buildProductDetail(item.findItemsByProductResponse.head.searchResult.get.head.item.head)
    }

}

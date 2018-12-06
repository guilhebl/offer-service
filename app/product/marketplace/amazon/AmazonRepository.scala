package product.marketplace.amazon

import common.config.AppConfigService
import common.executor.WorkerDispatcherContext
import common.log.ThreadLogger
import common.monitor.RequestMonitor
import javax.inject.{Inject, Singleton}
import play.api.Logger
import play.api.libs.ws._
import product.marketplace.common.MarketplaceConstants._
import product.marketplace.common.MarketplaceRepository
import product.model._

import scala.collection.mutable
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.xml.{Elem, NodeSeq}

trait AmazonRepository extends MarketplaceRepository

@Singleton
class AmazonRepositoryImpl @Inject()(
  ws: WSClient,
  appConfigService: AppConfigService,
  requestMonitor: RequestMonitor,
  helper: AmazonRequestHelper
)(implicit ec: WorkerDispatcherContext)
    extends AmazonRepository {

  private val logger = Logger(this.getClass)

  override def searchAll(req: ListRequest): Future[Option[OfferList]] = {
    searchAll(OfferList.empty(), req, appConfigService.properties("marketplaceDefaultTimeout").toInt)
  }

  override def search(request: ListRequest): Future[Option[OfferList]] = {
    ThreadLogger.log(s"Amazon Search $request")

    // match param names with specific provider params
    val params = filterParamsSearch(ListRequest.filterEmptyParams(request))

    // try to acquire lock from request Monitor
    if (!requestMonitor.isRequestPossible(Amazon)) {
      logger.info(s"Unable to acquire lock from Request Monitor")
      Future.successful(None)
    } else {
      search(params)
    }
  }

  private def search(params: Map[String, String]) = {
    val endpoint: String = appConfigService.properties("amazonUSEndpoint")
    val accessKeyId: String = appConfigService.properties("amazonUSaccessKeyId")
    val secretKey: String = appConfigService.properties("amazonUSsecretKey")
    val associateTag: String = appConfigService.properties("amazonUSassociateTag")
    val timeout = appConfigService.properties("marketplaceDefaultTimeout")

    val parameters = mutable.HashMap[String, String]()
    parameters.put("Service", "AWSECommerceService")
    parameters.put("Operation", "ItemSearch")
    parameters.put("AWSAccessKeyId", accessKeyId)
    parameters.put("AssociateTag", associateTag)
    parameters.put("SearchIndex", "All")
    parameters.put("ResponseGroup", "Images,ItemAttributes,Offers")
    parameters.put("ItemPage", params(Page))
    parameters.put("Keywords", params("Keywords"))
    val url = helper.sign(endpoint, accessKeyId, secretKey, parameters)

    logger.info("Amazon: " + url)

    val futureResult = ws.url(url).withRequestTimeout(timeout.toInt.millis).get().map { response =>
      buildList(params(Page).toInt, response.xml)
    }

    futureResult
  }

  override def getProductDetail(id: String, idType: String, source: String): Future[Option[OfferDetail]] = {
    Logger.info(s"getProductDetail: $id, $source")
    val idTypeAmazon = filterIdType(idType)

    // try to acquire lock from request Monitor
    if (!requestMonitor.isRequestPossible(Amazon) || idTypeAmazon.isEmpty) {
      logger.info(s"Unable to acquire lock from Request Monitor")
      Future.successful(None)
    } else {
      val endpoint: String = appConfigService.properties("amazonUSEndpoint")
      val accessKeyId: String = appConfigService.properties("amazonUSaccessKeyId")
      val secretKey: String = appConfigService.properties("amazonUSsecretKey")
      val associateTag: String = appConfigService.properties("amazonUSassociateTag")
      val timeout = appConfigService.properties("marketplaceDefaultTimeout")

      val parameters = mutable.HashMap[String, String]()
      parameters.put("Service", "AWSECommerceService")
      parameters.put("Operation", "ItemLookup")
      parameters.put("AWSAccessKeyId", accessKeyId)
      parameters.put("AssociateTag", associateTag)
      parameters.put("IdType", idTypeAmazon.get)
      parameters.put("ItemId", id)
      parameters.put("ResponseGroup", "Images,ItemAttributes,Offers")
      if (!idTypeAmazon.get.equals("ASIN")) {
        parameters.put("SearchIndex", "All")
      }
      val url = helper.sign(endpoint, accessKeyId, secretKey, parameters)

      logger.info(s"Amazon get by $idTypeAmazon: $url")
      val futureResult = ws.url(url).withRequestTimeout(timeout.toInt.millis).get().map { response =>
        buildProductDetail(response.xml)
      }
      futureResult
    }
  }

  private def filterIdType(str: String): Option[String] = {
    str match {
      case Id => Some("ASIN")
      case Upc => Some("UPC")
      case Ean => Some("EAN")
      case Isbn => Some("ISBN")
      case _ => None
    }
  }

  private def buildProductDetail(response: Elem): Option[OfferDetail] = {
    val items = response \ "Items"
    val itemNode = items \ "Item"
    if (itemNode.isEmpty) {
      None
    } else {
      val item = itemNode.head
      val proxyRequired = appConfigService.properties("marketplaceProvidersImageProxyRequired").indexOf(Amazon) != -1
      val itemAttrs = item \ "ItemAttributes"
      val offerSummary = item \ "OfferSummary"

      val detail = new OfferDetail(
        new Offer(
          (item \ "ASIN").text,
          Some((itemAttrs \ "UPC").text),
          (itemAttrs \ "Title").text,
          Amazon,
          (item \ "DetailPageURL").text,
          appConfigService.buildImgUrlExternal(Some((item \ "LargeImage" \ "URL").text), proxyRequired),
          appConfigService.buildImgUrl(Some("amazon-logo.png")),
          buildPrice(
            Some((offerSummary \ "LowestNewPrice" \ "FormattedPrice").text),
            Some((offerSummary \ "LowestUsedPrice" \ "FormattedPrice").text)
          ),
          (itemAttrs \ "ProductGroup").text,
          0.0f,
          0
        ),
        buildDescription(itemAttrs \ "features"),
        buildProductDetailAttributes(
          itemAttrs \ "Brand",
          itemAttrs \ "Manufacturer",
          itemAttrs \ "HardwarePlatform",
          itemAttrs \ "Model",
          itemAttrs \ "Publisher"
        ),
        Vector.empty[OfferDetailItem],
        Vector.empty[OfferPriceLog]
      )

      Some(detail)
    }
  }

  private def buildProductDetailAttributes(attrs: NodeSeq*): Vector[NameValue] = {
    attrs.filterNot(_.isEmpty).map(n => new NameValue(n.head.label, n.text)).toVector
  }

  private def buildDescription(node: NodeSeq): String = {
    if (node.isEmpty) {
      ""
    } else {
      val textList = node.map(n => n.text)
      textList.mkString
    }
  }

  /**
    * Matches param names with specifc provider names
    * @param params param with generic domain names
    * @return params matching specific param names to match provider API
    */
  private def filterParamsSearch(params: Map[String, String]): Map[String, String] = {
    val p = scala.collection.mutable.Map[String,String]()

    // get search keyword phrase
    if (params.contains(Name)) {
      p("Keywords") = params(Name)
    } else {
      p("Keywords") = getRandomSearchQuery(appConfigService.properties("amazonDefaultSearchQuery"), ",")
    }

    // get page - defaults to 1
    params.get(Page) match {
      case None => p(Page) = 1.toString
      case Some(v) => p(Page) = v
    }

    p.toMap
  }

  private def getRandomSearchQuery(query: String, separator: String) = {
    val strings = query.split(separator)
    val r = scala.util.Random
    strings(r.nextInt(strings.length))
  }

  private def buildList(page: Int, response: Elem): Option[OfferList] = {
    ThreadLogger.log("Amazon buildList")
    val items = response \ "Items"
    val total = (items \ "TotalResults").text.toInt
    if (total > 0) {
      val proxyRequired = appConfigService.properties("marketplaceProvidersImageProxyRequired").indexOf(Amazon) != -1
      val totalPages = (items \ "TotalPages").text.toInt
      val summary = new ListSummary(page, totalPages, total)

      val list = (items \ "Item").map { item =>
        val itemAttrs = item \ "ItemAttributes"
        val offerSummary = item \ "OfferSummary"
        new Offer(
          (item \ "ASIN").text,
          Some((itemAttrs \ "UPC").text),
          (itemAttrs \ "Title").text,
          Amazon,
          (item \ "DetailPageURL").text,
          appConfigService.buildImgUrlExternal(Some((item \ "LargeImage" \ "URL").text), proxyRequired),
          appConfigService.buildImgUrl(Some("amazon-logo.png")),
          buildPrice(
            Some((offerSummary \ "LowestNewPrice" \ "FormattedPrice").text),
            Some((offerSummary \ "LowestUsedPrice" \ "FormattedPrice").text)
          ),
          (itemAttrs \ "ProductGroup").text,
          0.0f,
          0
        )
      }

      val resp = new OfferList(list.toVector, summary)
      Some(resp)
    } else {
      None
    }
  }

  private def buildPrice(price: Option[String], usedPrice: Option[String]): Double = {
    val p = price.getOrElse(usedPrice.getOrElse("0"))
    val priceString = p.replaceAll("[^\\d.]", "")
    priceString.toDouble
  }

}

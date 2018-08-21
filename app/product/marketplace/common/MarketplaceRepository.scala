package product.marketplace.common

import java.util.Calendar

import common.cache.RedisCacheService
import common.config.AppConfigService
import common.db.MongoDbService
import common.executor.RepositoryDispatcherContext
import common.log.ThreadLogger
import common.util.CollectionUtil._
import common.util.EntityValidationUtil._
import common.util.StringCommonUtil._
import javax.inject.{Inject, Singleton}
import org.mongodb.scala.MongoCollection
import play.api.Logger
import play.api.libs.json.Json
import product.marketplace.amazon.AmazonRepository
import product.marketplace.bestbuy.BestBuyRepository
import product.marketplace.common.MarketplaceConstants._
import product.marketplace.ebay.EbayRepository
import product.marketplace.walmart.WalmartRepository
import product.model._

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.language.postfixOps
import scala.util.{Failure, Success}

/**
  * A pure non-blocking interface
  */
trait MarketplaceRepository {
  def search(req: ListRequest): Future[Option[OfferList]]
  def getProductDetail(id: String, idType: String, source: String, country: Option[String]): Future[Option[OfferDetail]]
}

@Singleton
class MarketplaceRepositoryImpl @Inject()(
  appConfigService: AppConfigService,
  walmartRepository: WalmartRepository,
  bestbuyRepository: BestBuyRepository,
  ebayRepository: EbayRepository,
  amazonRepository: AmazonRepository,
  mongoDbService: MongoDbService,
  cache: RedisCacheService
)(implicit ec: RepositoryDispatcherContext)
    extends MarketplaceRepository {

  private val logger = Logger(this.getClass)
  private val CollectionName = "offer"

  // utility functions to enable futures to be executed in parallel and later on wait for all to complete either with SUCCESS or FAILURE
  private def lift[T](futures: Seq[Future[T]]) =
    futures.map(_.map {
      Success(_)
    }.recover { case t => Failure(t) })

  private def waitAll[T](futures: Seq[Future[T]]) = Future.sequence(lift(futures))

  override def search(req: ListRequest): Future[Option[OfferList]] = {
    logger.info(s"Marketplace search - request: $req")
    // build request key
    val requestKey = Json.toJson(req).toString()
    // check cache
    if (appConfigService.properties("cache.enabled").toBoolean) {
      val json = cache.get(requestKey)
      if (json.isDefined) return Future.successful {
        Some(Json.parse(json.get).as[OfferList])
      }
    }

    val params = req.searchColumns.filterNot(_.value.isEmpty).map(x => (x.name, x.value)).toMap
    val country = if (params.contains(Country) && isValidCountry(params(Country))) params(Country) else UnitedStates
    val providers = getMarketplaceProvidersByCountry(country)
    val futures = providers.map(search(_, params))

    val result = waitAll(futures)
    val emptyList = Option(OfferList(Vector.empty, ListSummary(0, 0, 0)))

    result.map { x =>
      val l = x.foldLeft(emptyList)((r, c) => {
        if (c.isSuccess) mergeResponses(r, c.get) else r
      })
      val response = buildResponse(
        requestKey,
        l,
        country,
        params.getOrElse(Name, ""),
        req.sortColumn.getOrElse(""),
        req.sortOrder.getOrElse("").equals("asc")
      )
      response
    }
  }

  private def buildResponse(
    requestKey: String,
    offerList: Option[OfferList],
    country: String,
    keyword: String,
    sortBy: String,
    asc: Boolean
  ): Option[OfferList] = {
    val sorted = sortList(offerList, country, keyword, sortBy, asc)
    if (appConfigService.properties("db.enabled").toBoolean && sorted.isDefined) insertIntoDb(sorted.get)
    if (appConfigService.properties("cache.enabled").toBoolean && sorted.isDefined) insertIntoCache(requestKey, sorted.get)
    sorted
  }

  /**
    * check if DB is enabled and saves record
    *
    * @param item
    */
  private def insertIntoDb(item: OfferList): Future[Option[Seq[OfferLog]]] = {
    val cal = Calendar.getInstance()
    val seq = item.list.map(OfferLog(_, cal.getTimeInMillis)).toSeq

    val collection: MongoCollection[OfferLog] = mongoDbService.getDatabase.getCollection(CollectionName)
    collection.insertMany(seq).toFutureOption().map {
      case Some(_) =>
        logger.info("Offer Logs saved in Db")
        Some(seq)
      case _ => None
    }
  }

  /**
    * check if cache is enabled and saves record inside in-memory cache (redis)
    *
    * @param item
    */
  private def insertIntoCache(requestKey: String, item: OfferList): Unit = {
    logger.info(s"cache set: $requestKey")
    val obj = Json.toJson(item).toString()
    val savedResult = cache.set(requestKey, obj)
    if (!savedResult) logger.info("object not saved in cache")
  }

  private def sortList(offerList: Option[OfferList], country: String, keyword: String, sortBy: String, asc: Boolean): Option[OfferList] = {
    if (offerList.isEmpty) return None
    val list = offerList.get.list.toSeq

    val sorted = sortBy match {
      case Id => list.sortWith(if (asc) _.id < _.id else _.id > _.id)
      case Name => list.sortWith(if (asc) _.name.toLowerCase < _.name.toLowerCase else _.name.toLowerCase > _.name.toLowerCase)
      case Price => list.sortWith(if (asc) _.price < _.price else _.price > _.price)
      case Rating => list.sortWith(if (asc) _.rating < _.rating else _.rating > _.rating)
      case NumReviews => list.sortWith(if (asc) _.numReviews < _.numReviews else _.numReviews > _.numReviews)
      //case _ => if (!keyword.trim().equals("")) sortByBestResults(list, keyword.trim()) else sortGroupedByProvider(list, country)
      case _ => if (!keyword.trim().equals("")) sortByGroupedBestResults(list, keyword.trim(), country) else sortGroupedByProvider(list, country)
    }

    Some(
      new OfferList(
        sorted,
        offerList.get.summary
      )
    )
  }

  /**
    * Groups the results in buckets with each provider appearing in the first group of results
    *
    * @param offers
    * @return
    */
  private def sortGroupedByProvider(offers: Seq[Offer], country: String): Seq[Offer] = {
    val providers = getMarketplaceProvidersByCountry(country)
    val groups = offers.filter(p => providers.contains(p.partyName)).groupBy(_.partyName)
    val randomGroups = scala.util.Random.shuffle(groups)
    val lists = randomGroups.map(_._2.toList)
    val result = transposeLists(lists.toList).flatten
    result
  }

  /**
    * * Sorts using groups and filters out offers that don't have the keywords present
    *  moving them to last positions
    **/
  private def sortByGroupedBestResults(offers: Seq[Offer], str: String, country: String): Seq[Offer] = {
    val sortedByGroup = sortGroupedByProvider(offers, country)
    val keywords = str.toLowerCase()
    val listWithKeywords = sortedByGroup.filter(_.name.toLowerCase().indexOf(keywords) != -1)
    val listWithoutKeywords = sortedByGroup.diff(listWithKeywords)
    listWithKeywords ++ listWithoutKeywords
  }

  /**
    * /**
    * * Sorts with according preference order:
    * *
    * * 1. By unique matches of all keywords
    * * 2. by min Distacne between all keywords
    * * 3. by total matches of all keywords
    * * 4. by lowest index of first word found in text (the closest to the start the better)
    **/
    * @param offers
    * @param str
    * @return
    */
  private def sortByBestResults(offers: Seq[Offer], str: String): Seq[Offer] = {
    // filter keywords
    val keywords: Set[String] = str.split("\\s+").toSet

    val rankList = offers.map(o => {
      // search in Offer name (Title) for keywords and map it to a list of (Keyword, List(Indexes of matches in text))
      val matches = findAllSubstringMatchCount(o.name, keywords)
      val uniqueKeywords = matches.filterNot(_._2 == 0).length
      val totalMatches = matches.foldLeft(0)(_ + _._2)
      val minDistanceWords = getMinWindowSize(o.name, keywords)
      val firstIndexFirstWord = o.name.indexOf(keywords.head)
      OfferKeywordRank(o, uniqueKeywords, totalMatches, minDistanceWords, firstIndexFirstWord)
    })

    rankList.sortWith((or1, or2) => {
      if (or1.uniqueMatches != or2.uniqueMatches) {
        or1.uniqueMatches > or2.uniqueMatches
      } else if (or1.minDistanceWords != or2.minDistanceWords) {
        or1.minDistanceWords < or2.minDistanceWords
      } else if (or1.totalMatches != or2.totalMatches) {
        or1.minDistanceWords < or2.minDistanceWords
      } else {
        or1.lowestIndexFirstWord < or2.lowestIndexFirstWord
      }
    })

    val sortedOfferList = rankList.map(_.offer)
    sortedOfferList
  }

  override def getProductDetail(id: String, idType: String, source: String, country: Option[String]): Future[Option[OfferDetail]] = {
    logger.info(s"Marketplace get - params:  $id, $idType, $source, $country")
    val c = if (country.isDefined && isValidCountry(country.get)) country.get else UnitedStates
    if (isBlank(Some(id)) || !isValidMarketplaceIdType(idType) || !isValidMarketplaceProvider(c, source)) return Future.successful(None)

    // check cache
    if (appConfigService.properties("cache.enabled").toBoolean) {
      val json = cache.get(id)
      if (json.isDefined) return Future.successful {
        Some(Json.parse(json.get).as[OfferDetail])
      }
    }

    val timeout = appConfigService.properties("marketplaceAggregatorTimeout")
    val future = fetchProductDetail(id, idType, source, Some(c)).map { detail =>
      buildDetailResponseItems(detail, Upc, Some(c))
    } recover {
      case _: java.util.concurrent.TimeoutException => Future.successful(None)
    }

    Await.result(future, timeout.toInt millis)
  }

  private def isValidMarketplaceProvider(country: String, str: String): Boolean = {
    getMarketplaceProvidersByCountry(country).contains(str)
  }

  private def fetchProductDetail(id: String, idType: String, source: String, country: Option[String]): Future[Option[OfferDetail]] = {
    val future: Future[Option[OfferDetail]] = source match {
      case Walmart => walmartRepository.getProductDetail(id, idType, country)
      case BestBuy => bestbuyRepository.getProductDetail(id, idType, country)
      case Ebay => ebayRepository.getProductDetail(id, idType, country)
      case Amazon => amazonRepository.getProductDetail(id, idType, country)
      case _ => Future.successful(None)
    }
    future
  }

  private def getMarketplaceProvidersByCountry(country: String): Array[String] = {
    country match {
      case MarketplaceConstants.Canada => appConfigService.properties("marketplaceProvidersCanada").split(",")
      case _ => appConfigService.properties("marketplaceProviders").split(",")
    }
  }

  private def search(provider: String, params: Map[String, String]): Future[Option[OfferList]] = {
    provider match {
      case Walmart => walmartRepository.search(params)
      case BestBuy => bestbuyRepository.search(params)
      case Ebay => ebayRepository.search(params)
      case Amazon => amazonRepository.search(params)
      case _ => Future.successful(None)
    }
  }

  private def mergeResponses(response: Option[OfferList], response2: Option[OfferList]): Option[OfferList] = {
    ThreadLogger.log("Marketplace mergeResponse")

    if (response.isEmpty) return None
    if (response2.isEmpty) return response

    val page = if (response.get.summary.page == 0) response2.get.summary.page else response.get.summary.page

    Some(
      new OfferList(
        response.get.list ++ response2.get.list,
        new ListSummary(
          page,
          response.get.summary.pageCount + response2.get.summary.pageCount,
          response.get.summary.totalCount + response2.get.summary.totalCount
        )
      )
    )
  }

  private def mergeResponseProductDetail(response: Option[OfferDetail], response2: Option[OfferDetail]): Option[OfferDetail] = {
    if (response.isEmpty) return None
    if (response2.isEmpty) return response

    val item1 = response.get
    val item = response2.get

    // return a new merged OfferDetail obj.
    Some(
      OfferDetail(
        item1.offer,
        item1.description,
        item1.attributes,
        item1.productDetailItems ++ Seq(
          new OfferDetailItem(
            item.offer.partyName,
            item.offer.semanticName,
            item.offer.partyImageFileUrl,
            item.offer.price,
            item.offer.rating,
            item.offer.numReviews
          )
        )
      )
    )
  }

  private def buildDetailResponseItems(
    detail: Option[OfferDetail],
    idType: String,
    country: Option[String]
  ): Future[Option[OfferDetail]] = {
    val detailWithItems = getProductDetailItems(detail, idType, country)

    detailWithItems.map {
      case Some(x) =>
        if (appConfigService.properties("cache.enabled").toBoolean) {
          cache.set(x.offer.id, Json.toJson(x).toString())
        }
        Some(x)
      case _ => None
    }
  }

  /**
    * fetch product detail items from sources different than source (competitors other than original product source)
    */
  private def getProductDetailItems(detail: Option[OfferDetail], idType: String, country: Option[String]): Future[Option[OfferDetail]] = {
    if (detail.isEmpty || detail.get.offer.upc.isEmpty || isBlank(detail.get.offer.upc.get)) {
      return Future.successful(detail)
    }

    idType match {
      case Upc =>
        val providers = getMarketplaceProvidersByCountry(country.getOrElse(UnitedStates)).filter(!_.equals(detail.get.offer.partyName))
        val upc = detail.get.offer.upc.get
        val listFutures = for (provider <- providers) yield fetchProductDetail(upc, Upc, provider, country)
        val response = waitAll(listFutures)
        response.map { _.foldLeft(detail)((r, c) => { if (c.isSuccess) mergeResponseProductDetail(r, c.get) else r }) }

      case _ =>
        logger.error(s"Error - getProductDetailItems - invalid idType: $idType")
        Future.successful(None)
    }
  }

}

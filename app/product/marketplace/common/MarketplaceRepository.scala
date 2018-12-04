package product.marketplace.common

import java.util.Calendar

import common.cache.RedisCacheService
import common.config.AppConfigService
import common.db.MongoDbService
import common.executor.RepositoryDispatcherContext
import common.executor.model.BaseDomainRepository
import common.log.ThreadLogger
import common.util.CollectionUtil._
import common.util.DateUtil
import common.util.EntityValidationUtil._
import common.util.StringCommonUtil._
import javax.inject.{Inject, Singleton}
import org.mongodb.scala.MongoCollection
import org.mongodb.scala.model.Filters._
import org.mongodb.scala.model.Sorts.descending
import play.api.Logger
import play.api.libs.json.{Json, Writes}
import product.marketplace.amazon.AmazonRepository
import product.marketplace.bestbuy.BestBuyRepository
import product.marketplace.common.MarketplaceConstants._
import product.marketplace.ebay.EbayRepository
import product.marketplace.walmart.WalmartRepository
import product.model._

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.language.postfixOps

/**
  * A pure non-blocking interface
  */
trait MarketplaceRepository extends BaseDomainRepository {
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
  protected def searchAll(acc: OfferList, request: ListRequest, page: Int, timeout: Int): Future[Option[OfferList]] = {
    ThreadLogger.log(s"searchAll - request: $request, $page")
    val future = search(ListRequest.fromPage(request, page))
    val result = Await.result(future, timeout millis)
    val merged = OfferList.merge(Some(acc), result)

    // verify if above last page stop
    if (page + 1 > merged.get.summary.pageCount) {
      Future.successful(merged)
    } else {
      searchAll(merged.get, request, page + 1, timeout)
    }
  }
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
  private val CollectionOfferPriceLog = "offerLog"

  /**
    *  Searches in first page only
    *
    * @param req params
    * @return
    */
  override def search(req: ListRequest): Future[Option[OfferList]] = {
    search(req, searchSource)

  }

  /**
    * Searches in All pages
    *
    * @param req params
    * @return
    */
  override def searchAll(req: ListRequest): Future[Option[OfferList]] = {
    search(req, searchSourceAll)
  }

  /**
    * Internal search method called by both search modes (full and simple)
    *
    * @param req param
    * @param all if full search
    * @return result
    */
  private def search(req: ListRequest, f: (String, ListRequest) => Future[Option[OfferList]]): Future[Option[OfferList]] = {
    ThreadLogger.log(s"Marketplace search - request: $req")

    // build request key
    val requestKey = Json.toJson(req).toString()
    // check cache
    if (appConfigService.properties("cache.enabled").toBoolean) {
      val json = cache.get(requestKey)
      if (json.isDefined) Future.successful(Some(Json.parse(json.get).as[OfferList])) else Future.successful(None)
    } else {
      val providers = getMarketplaceProviders()
      val futures = for (p <- providers) yield f(p, req)
      val result = waitAll(futures)
      val emptyList = Option(OfferList(Vector.empty, ListSummary(0, 0, 0)))

      result.map { x =>
        val result = x.foldLeft(emptyList)((r, c) => {
          if (c.isSuccess) OfferList.merge(r, c.get) else r
        })
        buildEntityResult(requestKey, buildResponse(req, result))
      }
    }
  }

  /**
    * Builds sorted List response
    *
    * @param requestKey json representation of request
    * @param request params object
    * @param offerList response
    * @return
    */
  private def buildResponse(
    request: ListRequest,
    offerList: Option[OfferList]
  ): OfferList = {

    val mapParams = ListRequest.filterEmptyParams(request)
    val country = ListRequest.filterCountry(mapParams)
    val keyword = mapParams.getOrElse(Name, "")
    val sortBy = request.sortColumn.getOrElse("")
    val asc = request.sortOrder.getOrElse("asc").equals("asc")
    val sorted = sortList(offerList, country, keyword, sortBy, asc)

    if (appConfigService.properties("db.enabled").toBoolean && sorted.list.nonEmpty) insertOfferPriceLogs(sorted)
    sorted
  }

  /**
    * if cache is enabled saves in cache first as JSON then returns value wrapped as an Option
    *
    * @param detail result object
    * @return option of device detail
    */
  private def buildEntityResult[T: Writes](requestKey: String, obj: T): Option[T] = {
    updateCache(requestKey, Json.toJson(obj).toString())
    Some(obj)
  }

  /**
    * if cache is enabled saves in cache then returns same value Option
    *
    * @param detail result option
    * @return result
    */
  private def buildEntityResult[T: Writes](requestKey: String, obj: Option[T]): Option[T] = {
    updateCache(requestKey, Json.toJson(obj).toString())
    obj
  }

  /**
    * if cache is enabled saves in cache as JSON
    *
    * @param key key to cache
    * @param json value of mapping
    */
  private def updateCache(key: String, json: String): Unit = {
    if (appConfigService.properties("cache.enabled").toBoolean) {
      cache.set(key, json)
    }
  }

  /**
    * check if DB is enabled and saves record
    *
    * inserts a log of each single offer in list
    *
    * - a snapshot of the price of the object in time
    * - only inserts if offer has UPC
    *
    * @param item OfferList
    */
  private def insertOfferPriceLogs(item: OfferList): Unit = {
    logger.info("Insert OfferPriceLogs")
    val cal = Calendar.getInstance()
    val offersWithUpc = item.list.withFilter(_.upc.isDefined)

    offersWithUpc.foreach(x => {
      x.upc match {
        case Some(upc) =>
          insertOfferPriceLog(
            OfferPriceLog(upc, x.partyName, x.price, cal.getTimeInMillis)
          )
      }
    })
  }

  /**
    * Gets the offer price logs for this upc
    *
    * @param upc upc to search in DB
    * @return
    */
  private def getOfferPriceLogs(upc: String): Vector[OfferPriceLog] = {
    if (appConfigService.properties("db.enabled").toBoolean) {
      val collection: MongoCollection[OfferPriceLog] = mongoDbService.getDatabase.getCollection(CollectionOfferPriceLog)

      val f = collection
        .find(equal("upc", upc))
        .sort(descending("timestamp"))
        .toFuture()

      val timeout = appConfigService.properties("mongoDb.defaultTimeout")
      val offerLogs = Await.result(f, timeout.toInt millis)
      offerLogs.toVector
    } else {
      Vector.empty[OfferPriceLog]
    }
  }

  /**
    * Gets the last inserted offer price log for this upc
    *
    * @param upc upc to search in DB
    * @return
    */
  private def getLastOfferPriceLog(upc: String): Option[OfferPriceLog] = {
    if (appConfigService.properties("db.enabled").toBoolean) {
      val collection: MongoCollection[OfferPriceLog] = mongoDbService.getDatabase.getCollection(CollectionOfferPriceLog)
      val f = collection
        .find(equal("upc", upc))
        .sort(descending("timestamp"))
        .limit(1)
        .toFuture()

      val timeout = appConfigService.properties("mongoDb.defaultTimeout")
      val offerLogs = Await.result(f, timeout.toInt millis)
      offerLogs.headOption

    } else {
      None
    }
  }

  /**
    * check if last price log is past within timewindow T
    * and if not inserts a new price log.
    *
    * for example checks to see if the last price log saved in DB was 24h or more
    * if so proceeds and saves new record offer price log
    *
    * @param item OfferPriceLog
    */
  private def insertOfferPriceLog(item: OfferPriceLog): Unit = {
    logger.info(s"Insert OfferPrice Log " + item.upc)
    val lastPriceLog = getLastOfferPriceLog(item.upc)
    val cycleSeconds = appConfigService.properties("offer.priceLog.timeWindow").toLong
    if (lastPriceLog.isDefined && !DateUtil.isBeforeSeconds(lastPriceLog.get.timestamp, cycleSeconds)) {
      logger.info(s"last price log within cycle of $cycleSeconds seconds - skipping " + item.upc)
    } else {
      insertOfferPriceLogDb(item)
    }
  }

  /**
    * check if DB is enabled and saves record
    *
    * inserts a OfferPriceLog of offer
    *
    * @param item OfferPriceLog
    */
  private def insertOfferPriceLogDb(item: OfferPriceLog): Unit = {
    logger.info(s"Insert OfferPrice Log DB " + item.upc)
    val collection: MongoCollection[OfferPriceLog] = mongoDbService.getDatabase.getCollection(CollectionOfferPriceLog)
    val f = collection.insertOne(item).toFutureOption().map {
      case Some(_) => logger.info("Record Inserted in Db")
      case _ => logger.info("ERROR: Failed to Insert record in Db!")
    }

    val timeout = appConfigService.properties("mongoDb.defaultTimeout")
    Await.result(f, timeout.toInt millis)
  }

  /**
    * check if DB is enabled and deletes all price logs with current date past 1 timewindow (defaults to 1 month)
    *
    */
  private def deleteOldOfferPriceLogs(): Future[Long] = {
    logger.info("delete old price logs")
    val collection: MongoCollection[OfferPriceLog] = mongoDbService.getDatabase.getCollection(CollectionOfferPriceLog)
    val timeout = appConfigService.properties("mongoDb.defaultTimeout")
    val startOfMonth = DateUtil.getStartOfMonthTimestamp()

    // get all
    val f = collection.find(lte("timestamp", startOfMonth)).toFuture()
    val result = Await.result(f, timeout.toInt millis)
    result.foreach(r => logger.info(s"Delete: $r"))

    // remove all items before start of month
    val timestamps = result.map(_.timestamp)
    val future = collection.deleteMany(in("timestamp", timestamps: _*)).toFuture().map { x =>
      logger.info("Deleted " + x.getDeletedCount + " records")
      x.getDeletedCount
    }

    future
  }

  /**
    * Sorts list according to various fields
    *
    * @param offerList list to be sorted
    * @param country country
    * @param keyword keyword
    * @param sortBy sortBy string
    * @param asc sort order if "asc" = true otherwise false
    * @return sorted list
    */
  private def sortList(offerList: Option[OfferList], country: String, keyword: String, sortBy: String, asc: Boolean): OfferList = {
    val list: Vector[Offer] = offerList.get.list

    val sorted = sortBy match {
      case Id => list.sortWith(if (asc) _.id < _.id else _.id > _.id)
      case Name => list.sortWith(if (asc) _.name.toLowerCase < _.name.toLowerCase else _.name.toLowerCase > _.name.toLowerCase)
      case Price => list.sortWith(if (asc) _.price < _.price else _.price > _.price)
      case Rating => list.sortWith(if (asc) _.rating < _.rating else _.rating > _.rating)
      case NumReviews => list.sortWith(if (asc) _.numReviews < _.numReviews else _.numReviews > _.numReviews)
      case _ =>
        if (!keyword.trim().equals("")) sortByGroupedBestResults(list, keyword.trim(), country) else sortGroupedByProvider(list, country)
    }

    OfferList(
      sorted.toVector,
      offerList.get.summary
    )
  }

  /**
    * Groups the results in buckets with each provider appearing in the first group of results
    *
    * @param offers collection of offers
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

  override def getProductDetail(id: String, idType: String, source: String): Future[Option[OfferDetail]] = {
    logger.info(s"Marketplace get - params:  $id, $idType, $source")
    if (isBlank(Some(id)) || !isValidMarketplaceIdType(idType) || !isValidMarketplaceProvider(source)) {
      Future.successful(None)
    } else {
      val cacheKey = OfferDetail.buildCacheKey(id)
      val json = if (appConfigService.properties("cache.enabled").toBoolean) cache.get(cacheKey) else None
      if (json.isDefined) {
        Future.successful(Some(Json.parse(json.get).as[OfferDetail]))
      } else {
        val timeout = appConfigService.properties("marketplaceAggregatorTimeout")
        fetchProductDetail(id, idType, source).map(
          detail =>
            buildEntityResult(
              cacheKey,
              Await.result(fetchDetailItems(detail, Upc), timeout.toInt millis)
          )
        )
      }
    }
  }

  /**
    * Decorates an Offer Detail fetching and adding additional info for this offer detail such as
    *
    * - detail items from other sources using same UPC
    * - analytics contained in last log using UPC as param
    *
    * @param detail
    * @param idType
    * @param country
    * @return
    */
  private def fetchDetailItems(
    detail: Option[OfferDetail],
    idType: String
  ): Future[Option[OfferDetail]] = {

    val detailWithItems = getProductDetailItems(detail, idType)

    detailWithItems.map { x =>
      if (x.isDefined && x.get.offer.upc.isDefined) {
        val offerPriceLogs = getOfferPriceLogs(x.get.offer.upc.get)
        Some(OfferDetail(x.get.offer, x.get.description, x.get.attributes, x.get.productDetailItems, offerPriceLogs))
      } else {
        x
      }
    }
  }

  /**
    * fetch product detail items from sources different than source (competitors other than original product source)
    */
  private def getProductDetailItems(detail: Option[OfferDetail], idType: String): Future[Option[OfferDetail]] = {
    if (detail.isEmpty || detail.get.offer.upc.isEmpty || isBlank(detail.get.offer.upc.get)) {
      Future.successful(detail)
    } else {
      idType match {
        case Upc =>
          val providers = getMarketplaceProviders().filter(!_.equals(detail.get.offer.partyName))
          val upc = detail.get.offer.upc.get
          val listFutures = for (provider <- providers) yield fetchProductDetail(upc, Upc, provider)
          val response = waitAll(listFutures)
          response.map { _.foldLeft(detail)((r, c) => { if (c.isSuccess) OfferDetail.mergeOption(r, c.get) else r }) }

        case _ =>
          logger.error(s"getProductDetailItems - No UPC found. IdType: $idType")
          Future.successful(None)
      }
    }
  }

  private def fetchProductDetail(id: String, idType: String, source: String): Future[Option[OfferDetail]] = {
    val future: Future[Option[OfferDetail]] = source match {
      case Walmart => walmartRepository.getProductDetail(id, idType, source)
      case BestBuy => bestbuyRepository.getProductDetail(id, idType, source)
      case Ebay => ebayRepository.getProductDetail(id, idType, source)
      case Amazon => amazonRepository.getProductDetail(id, idType, source)
      case _ => Future.successful(None)
    }
    future
  }

  /**
    *  Gets default provider country - USA marketplace providers
    *
    * @return an array containing each provider of USA market
    */
  private def getMarketplaceProviders(): Array[String] = {
    appConfigService.properties("marketplaceProviders").split(",")
  }

  private def getMarketplaceProvidersByCountry(country: String): Array[String] = {
    country match {
      case MarketplaceConstants.Canada => appConfigService.properties("marketplaceProvidersCanada").split(",")
      case _ => appConfigService.properties("marketplaceProviders").split(",")
    }
  }

  /**
    * Searches in a single source provider and gets results from single page only
    *
    * @param source the source provider name
    * @param request the list request
    * @return
    */
  private def searchSource(source: String, request: ListRequest): Future[Option[OfferList]] = {
    source match {
      case Walmart => walmartRepository.search(request)
      case BestBuy => bestbuyRepository.search(request)
      case Ebay => ebayRepository.search(request)
      case Amazon => amazonRepository.search(request)
      case _ => Future.successful(None)
    }
  }

  /**
    * Searches in a single source provider for all pages
    *
    * @param source the source provider name
    * @param request the list request
    * @return
    */
  private def searchSourceAll(source: String, request: ListRequest): Future[Option[OfferList]] = {
    source match {
      case Walmart => walmartRepository.searchAll(request)
      case BestBuy => bestbuyRepository.searchAll(request)
      case Ebay => ebayRepository.searchAll(request)
      case Amazon => amazonRepository.searchAll(request)
      case _ => Future.successful(None)
    }
  }

  private def isValidMarketplaceProvider(str: String): Boolean = isValidMarketplaceProvider(UnitedStates, str)
  private def isValidMarketplaceProvider(country: String, str: String): Boolean = getMarketplaceProvidersByCountry(country).contains(str)

}

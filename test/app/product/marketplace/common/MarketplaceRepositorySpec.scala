package app.product.marketplace.common

import common.MockBaseUtil._
import common.cache.RedisCacheService
import common.config.AppConfigService
import common.db.MongoDbService
import org.junit.runner.RunWith
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.junit.JUnitRunner
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.Json
import product.marketplace.amazon.AmazonRepository
import product.marketplace.bestbuy.BestBuyRepository
import product.marketplace.common.MarketplaceConstants._
import product.marketplace.common.MarketplaceRepositoryImpl
import product.marketplace.ebay.EbayRepository
import product.marketplace.walmart.WalmartRepository
import product.model.{ListRequest, ListSummary, OfferDetail, OfferList}

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps

@RunWith(classOf[JUnitRunner])
class MarketplaceRepositorySpec extends PlaySpec with MockitoSugar with ScalaFutures {

  val emptyList = Option(new OfferList(Vector.empty, new ListSummary(0, 0, 0)))

    /**
      * a template function for sort tests
      *
      * @param sortBy column to sort
      * @param sortOrder order to sort
      * @return
      */
    def testSortBy(sortBy:String, sortOrder: String) : Option[OfferList] = {

      val walmartMock = mock[WalmartRepository]
      when(walmartMock.search(any[ListRequest])) thenReturn Future.successful(Some(getProductList))

      val bestbuyMock = mock[BestBuyRepository]
      when(bestbuyMock.search(any[ListRequest])) thenReturn Future.successful(Some(getProductList))

      val ebayMock = mock[EbayRepository]
      when(ebayMock.search(any[ListRequest])) thenReturn Future.successful(Some(getProductList))

      val amazonMock = mock[AmazonRepository]
      when(amazonMock.search(any[ListRequest])) thenReturn Future.successful(Some(getProductList))

      val repositoryDispatcher = getMockExecutionContext

      val mongoRepositoryMock = mock[MongoDbService]
      val redisCacheServiceMock = mock[RedisCacheService]

      val req = new ListRequest(Vector.empty, Some(sortBy), Some(sortOrder), Some(1), Some(10))

      val cacheKey = Json.toJson(req).toString()
      when(redisCacheServiceMock.get(cacheKey))thenReturn None

      val appConfigMock = mock[AppConfigService]
      when(appConfigMock.properties) thenReturn testConfigProperties

      val service = new MarketplaceRepositoryImpl(appConfigMock, walmartMock, bestbuyMock, ebayMock, amazonMock,
        mongoRepositoryMock, redisCacheServiceMock)(repositoryDispatcher)

      val response = service.search(req)

      whenReady(response, timeout(defaultTestAsyncAwaitTimeout seconds), interval(defaultTestAsyncInterval millis)) {
        r: Option[OfferList] =>
          // verify 1 call made to each provider
          verify(bestbuyMock, times(1)).search(any[ListRequest])
          verify(walmartMock, times(1)).search(any[ListRequest])
          verify(ebayMock, times(1)).search(any[ListRequest])
          verify(amazonMock, times(1)).search(any[ListRequest])
          r
      }
    }

    /**
      * Validates default summary results
      *
      * @param summary a list summary
      */
    def validateDefaultSummary(summary: ListSummary): Unit = {
      summary.page mustBe 1
      summary.pageCount mustBe 1
      summary.totalCount mustBe 16
    }

  "search" should {
    "be valid when there are results" in {
      val walmartMock = mock[WalmartRepository]
      when(walmartMock.search(any[ListRequest])) thenReturn Future.successful(Some(getProductList))

      val bestbuyMock = mock[BestBuyRepository]
      when(bestbuyMock.search(any[ListRequest])) thenReturn Future.successful(Some(getProductList))

      val ebayMock = mock[EbayRepository]
      when(ebayMock.search(any[ListRequest])) thenReturn Future.successful(Some(getProductList))

      val amazonMock = mock[AmazonRepository]
      when(amazonMock.search(any[ListRequest])) thenReturn Future.successful(Some(getProductList))

      val mongoRepositoryMock = mock[MongoDbService]
      val redisCacheServiceMock = mock[RedisCacheService]
      val repositoryDispatcher = getMockExecutionContext

      val appConfigMock = mock[AppConfigService]
      when(appConfigMock.properties) thenReturn testConfigProperties

      val service = new MarketplaceRepositoryImpl(appConfigMock, walmartMock, bestbuyMock, ebayMock, amazonMock, mongoRepositoryMock,
        redisCacheServiceMock)(repositoryDispatcher)
      when(redisCacheServiceMock.get("{\"searchColumns\":[]}")) thenReturn None

      val req = new ListRequest()
      val response = service.search(req)

      whenReady(response, timeout(defaultTestAsyncAwaitTimeout seconds), interval(defaultTestAsyncInterval millis)) {
        r: Option[OfferList] =>
          val offerlist = r.get
          val summary = offerlist.summary
          validateDefaultSummary(summary)

          // verify 1 call made to each provider
          verify(bestbuyMock, times(1)).search(any[ListRequest])
          verify(walmartMock, times(1)).search(any[ListRequest])
          verify(ebayMock, times(1)).search(any[ListRequest])
          verify(amazonMock, times(1)).search(any[ListRequest])
      }

    }

    "sorted by lowest price" in {
      val result = testSortBy(Price, "asc")
      val offerList = result.get
      validateDefaultSummary(offerList.summary)
      offerList.list.head.price mustBe 55.55D
    }

    "sorted by highest price" in {
      val result = testSortBy(Price, "desc")
      val offerList = result.get
      validateDefaultSummary(offerList.summary)
      offerList.list.head.price mustBe 445.55D
    }

    "sorted by name asc" in {
      val result = testSortBy(Name, "asc")
      val offerList = result.get
      validateDefaultSummary(offerList.summary)
      offerList.list.head.name.charAt(0) mustBe 'A'
    }

    "sorted by name desc" in {
      val result = testSortBy(Name, "desc")
      val offerList = result.get
      validateDefaultSummary(offerList.summary)
      offerList.list.head.name.charAt(0) mustBe 'Z'
    }

    "sorted by Id asc" in {
      val result = testSortBy(Id, "asc")
      val offerList = result.get
      validateDefaultSummary(offerList.summary)
      offerList.list.head.id mustBe "1"
    }

    "sorted by Id desc" in {
      val result = testSortBy(Id, "desc")
      val offerList = result.get
      validateDefaultSummary(offerList.summary)
      offerList.list.head.id mustBe "4"
    }

    "sorted by Rating asc" in {
      val result = testSortBy(Rating, "asc")
      val offerList = result.get
      validateDefaultSummary(offerList.summary)
      offerList.list.head.rating mustBe 2.55f
    }

    "sorted by Rating desc" in {
      val result = testSortBy(Rating, "desc")
      val offerList = result.get
      validateDefaultSummary(offerList.summary)
      offerList.list.head.rating mustBe 7.55f
    }

    "sorted by NumReviews asc" in {
      val result = testSortBy(NumReviews, "asc")
      val offerList = result.get
      validateDefaultSummary(offerList.summary)
      offerList.list.head.numReviews mustBe 100
    }

    "sorted by NumReviews desc" in {
      val result = testSortBy(NumReviews, "desc")
      val offerList = result.get
      validateDefaultSummary(offerList.summary)
      offerList.list.head.numReviews mustBe 300
    }

    "be valid and merge only first result if no other results available" in {
      val walmartMock = mock[WalmartRepository]
      when(walmartMock.search(any[ListRequest])) thenReturn Future.successful(Some(getProductList))

      val bestbuyMock = mock[BestBuyRepository]
      when(bestbuyMock.search(any[ListRequest])) thenReturn Future.successful(None)

      val ebayMock = mock[EbayRepository]
      when(ebayMock.search(any[ListRequest])) thenReturn Future.successful(None)

      val amazonMock = mock[AmazonRepository]
      when(amazonMock.search(any[ListRequest])) thenReturn Future.successful(None)

      val repositoryDispatcher = getMockExecutionContext

      val appConfigMock = mock[AppConfigService]
      when(appConfigMock.properties) thenReturn testConfigProperties

      val mongoRepositoryMock = mock[MongoDbService]
      val redisCacheServiceMock = mock[RedisCacheService]
      when(redisCacheServiceMock.get("{\"searchColumns\":[]}")) thenReturn None

      val service = new MarketplaceRepositoryImpl(appConfigMock, walmartMock, bestbuyMock, ebayMock, amazonMock, mongoRepositoryMock,
        redisCacheServiceMock)(repositoryDispatcher)

      val response = service.search(new ListRequest())

      whenReady(response, timeout(defaultTestAsyncAwaitTimeout seconds), interval(defaultTestAsyncInterval millis)) {
        r: Option[OfferList] =>
          val list = r.get
          val summary = list.summary

          summary.page mustBe 1
          summary.pageCount mustBe 1
          summary.totalCount mustBe 4
          list.list.size mustBe 4

          // verify 1 call made to each provider
          verify(bestbuyMock, times(1)).search(any[ListRequest])
          verify(walmartMock, times(1)).search(any[ListRequest])
          verify(ebayMock, times(1)).search(any[ListRequest])
          verify(amazonMock, times(1)).search(any[ListRequest])
      }

    }

    "be empty when there are no results" in {
      val walmartMock = mock[WalmartRepository]
      when(walmartMock.search(any[ListRequest])) thenReturn Future.successful(None)

      val bestbuyMock = mock[BestBuyRepository]
      when(bestbuyMock.search(any[ListRequest])) thenReturn Future.successful(None)

      val ebayMock = mock[EbayRepository]
      when(ebayMock.search(any[ListRequest])) thenReturn Future.successful(None)

      val amazonMock = mock[AmazonRepository]
      when(amazonMock.search(any[ListRequest])) thenReturn Future.successful(None)

      val repositoryDispatcher = getMockExecutionContext

      val appConfigMock = mock[AppConfigService]
      when(appConfigMock.properties) thenReturn testConfigProperties

      val mongoRepositoryMock = mock[MongoDbService]
      val redisCacheServiceMock = mock[RedisCacheService]
      when(redisCacheServiceMock.get("{\"searchColumns\":[]}")) thenReturn None

      val service = new MarketplaceRepositoryImpl(appConfigMock, walmartMock, bestbuyMock, ebayMock, amazonMock, mongoRepositoryMock, redisCacheServiceMock)(repositoryDispatcher)

      val response = service.search(new ListRequest())

      whenReady(response, timeout(defaultTestAsyncAwaitTimeout seconds), interval(defaultTestAsyncInterval millis)) {
        r: Option[OfferList] =>
          val list = r.get
          val summary = list.summary

          summary.page mustBe 0
          summary.pageCount mustBe 0
          summary.totalCount mustBe 0
          list.list.isEmpty mustBe true

          // verify 1 call made to each provider
          verify(bestbuyMock, times(1)).search(any[ListRequest])
          verify(walmartMock, times(1)).search(any[ListRequest])
          verify(ebayMock, times(1)).search(any[ListRequest])
          verify(amazonMock, times(1)).search(any[ListRequest])
      }
    }

  }

  "getProductDetail" should {
    "be valid and not empty and fetch other competitors prices when Upc is present and there are results" in {
      val bestbuyMock = mock[BestBuyRepository]
      when(bestbuyMock.getProductDetail("1", Id, BestBuy)) thenReturn {
        Future.successful(Some(getProductDetailNoItems))
      }

      val walmartMock = mock[WalmartRepository]
      when(walmartMock.getProductDetail("upc1", Upc, Walmart)) thenReturn Future.successful(Some(getProductDetailNoItemsParty2))

      val ebayMock = mock[EbayRepository]
      when(ebayMock.getProductDetail("upc1", Upc, Ebay)) thenReturn Future.successful(Some(getProductDetailNoItemsParty2))

      val amazonMock = mock[AmazonRepository]
      when(amazonMock.getProductDetail("upc1", Upc, Amazon)) thenReturn Future.successful(Some(getProductDetailNoItemsParty2))

      val repositoryDispatcher = getMockExecutionContext

      val appConfigMock = mock[AppConfigService]
      when(appConfigMock.properties) thenReturn testConfigProperties

      val mongoRepositoryMock = mock[MongoDbService]
      val redisCacheServiceMock = mock[RedisCacheService]
      when(redisCacheServiceMock.get("1")) thenReturn None

      val service = new MarketplaceRepositoryImpl(appConfigMock, walmartMock, bestbuyMock, ebayMock, amazonMock, mongoRepositoryMock, redisCacheServiceMock)(repositoryDispatcher)

      val response = service.getProductDetail("1", Id, BestBuy)

      whenReady(response, timeout(defaultTestAsyncAwaitTimeout seconds), interval(defaultTestAsyncInterval millis)) {
        r: Option[OfferDetail] =>
          val detail = r.get
          detail.offer.id mustBe "1"
          detail.offer.upc mustBe Some("upc1")
          detail.offer.name mustBe "prod 1"
          detail.offer.semanticName mustBe "sem1"
          detail.offer.mainImageFileUrl mustBe "http://localhost:5555/assets/images/image-placeholder.png"
          detail.offer.partyName mustBe BestBuy
          detail.offer.partyImageFileUrl mustBe "http://localhost:5555/assets/images/best-buy-logo.png"
          detail.offer.price mustBe 55.55D
          detail.offer.productCategory mustBe "laptops"
          detail.offer.rating mustBe 4.5f
          detail.offer.numReviews mustBe 100
          detail.description mustBe "The new MacBook Pro is faster and more powerful than before."

          val attr0 = detail.attributes.head
          attr0.name mustBe "model"
          attr0.value mustBe "Radeon Pro 455"

          val detailItem0 = detail.productDetailItems.head
          detailItem0.partyName mustBe "pty 2"
          detailItem0.price mustBe 52.31
          detailItem0.numReviews mustBe 345
          detailItem0.rating mustBe 3.1f

          // verify 1 call made to BestBuy
          verify(bestbuyMock, times(1)).getProductDetail("1", Id, BestBuy)

          // verify other competitors called since UPC is present
          verify(walmartMock, times(1)).getProductDetail(anyString, anyString, anyString)
          verify(ebayMock, times(1)).getProductDetail(anyString, anyString, anyString)
          verify(amazonMock, times(1)).getProductDetail(anyString, anyString, anyString)
      }
    }

    "be valid and not empty and not fetch other competitors prices when Upc is not present and there are results" in {
      val bestbuyMock = mock[BestBuyRepository]
      when(bestbuyMock.getProductDetail("1", Id, BestBuy)) thenReturn Future.successful(Some(getProductDetailNoItemsNoUpc))

      val walmartMock = mock[WalmartRepository]
      val ebayMock = mock[EbayRepository]
      val amazonMock = mock[AmazonRepository]
      val repositoryDispatcher = getMockExecutionContext

      val mongoRepositoryMock = mock[MongoDbService]
      val redisCacheServiceMock = mock[RedisCacheService]
      when(redisCacheServiceMock.get("1")) thenReturn None

      val appConfigMock = mock[AppConfigService]
      when(appConfigMock.properties) thenReturn testConfigProperties

      val service = new MarketplaceRepositoryImpl(appConfigMock, walmartMock, bestbuyMock, ebayMock, amazonMock, mongoRepositoryMock, redisCacheServiceMock)(repositoryDispatcher)

      val response = service.getProductDetail("1", Id, BestBuy)

      whenReady(response, timeout(defaultTestAsyncAwaitTimeout seconds), interval(defaultTestAsyncInterval millis)) {
        r: Option[OfferDetail] =>
          val detail = r.get
          detail.offer.id mustBe "1"
          detail.offer.upc mustBe Some("")
          detail.offer.name mustBe "prod 1"
          detail.offer.semanticName mustBe "sem1"
          detail.offer.mainImageFileUrl mustBe "http://localhost:5555/assets/images/image-placeholder.png"
          detail.offer.partyName mustBe BestBuy
          detail.offer.partyImageFileUrl mustBe "http://localhost:5555/assets/images/best-buy-logo.png"
          detail.offer.price mustBe 31.55D
          detail.offer.productCategory mustBe "laptops"
          detail.offer.rating mustBe 3.5f
          detail.offer.numReviews mustBe 400
          detail.description mustBe "The new MacBook Pro is faster and more powerful than before."
          detail.productDetailItems.isEmpty mustBe true

          val attr0 = detail.attributes.head
          attr0.name mustBe "model"
          attr0.value mustBe "Radeon Pro 455"

          // verify 1 call made to BestBuy
          verify(bestbuyMock, times(1)).getProductDetail("1", Id, BestBuy)

          // verify other competitors not called since no UPC is present
          verify(walmartMock, times(0)).getProductDetail(anyString, anyString, anyString)
          verify(ebayMock, times(0)).getProductDetail(anyString, anyString, anyString)
          verify(amazonMock, times(0)).getProductDetail(anyString, anyString, anyString)
      }
    }

    "be empty when there are no results" in {
      val bestbuyMock = mock[BestBuyRepository]
      when(bestbuyMock.getProductDetail("1", Id, BestBuy)) thenReturn Future.successful(None)

      val walmartMock = mock[WalmartRepository]
      val ebayMock = mock[EbayRepository]
      val amazonMock = mock[AmazonRepository]
      val repositoryDispatcher = getMockExecutionContext

      val appConfigMock = mock[AppConfigService]
      when(appConfigMock.properties) thenReturn testConfigProperties

      val mongoRepositoryMock = mock[MongoDbService]
      val redisCacheServiceMock = mock[RedisCacheService]
      when(redisCacheServiceMock.get("1")) thenReturn None

      val service = new MarketplaceRepositoryImpl(appConfigMock, walmartMock, bestbuyMock, ebayMock, amazonMock, mongoRepositoryMock,
        redisCacheServiceMock)(repositoryDispatcher)

      val response = service.getProductDetail("1", Id, BestBuy)

      whenReady(response, timeout(defaultTestAsyncAwaitTimeout seconds), interval(defaultTestAsyncInterval millis)) {
        r: Option[OfferDetail] =>
          r mustBe None

          // verify 1 call made to BestBuy
          verify(bestbuyMock, times(1)).getProductDetail("1", Id, BestBuy)

          // verify other competitors not called
          verify(walmartMock, times(0)).getProductDetail(anyString, anyString, anyString)
          verify(ebayMock, times(0)).getProductDetail(anyString, anyString, anyString)
          verify(amazonMock, times(0)).getProductDetail(anyString, anyString, anyString)
      }
    }

    "be empty when empty Id is sent as param" in {
      val bestbuyMock = mock[BestBuyRepository]
      val walmartMock = mock[WalmartRepository]
      val ebayMock = mock[EbayRepository]
      val amazonMock = mock[AmazonRepository]
      val repositoryDispatcher = getMockExecutionContext
      val mongoRepositoryMock = mock[MongoDbService]
      val redisCacheServiceMock = mock[RedisCacheService]

      val appConfigMock = mock[AppConfigService]
      when(appConfigMock.properties) thenReturn testConfigProperties

      val service = new MarketplaceRepositoryImpl(appConfigMock, walmartMock, bestbuyMock, ebayMock, amazonMock,
        mongoRepositoryMock, redisCacheServiceMock)(repositoryDispatcher)

      val response = service.getProductDetail("", Id, BestBuy)

      whenReady(response, timeout(defaultTestAsyncAwaitTimeout seconds), interval(defaultTestAsyncInterval millis)) {
        r: Option[OfferDetail] =>
          r mustBe None

          // verify other competitors not called
          verify(bestbuyMock, times(0)).getProductDetail(anyString, anyString, anyString)
          verify(walmartMock, times(0)).getProductDetail(anyString, anyString, anyString)
          verify(ebayMock, times(0)).getProductDetail(anyString, anyString, anyString)
          verify(amazonMock, times(0)).getProductDetail(anyString, anyString, anyString)
      }
    }

    "be empty when empty IdType is sent as param" in {
      val bestbuyMock = mock[BestBuyRepository]
      val walmartMock = mock[WalmartRepository]
      val ebayMock = mock[EbayRepository]
      val amazonMock = mock[AmazonRepository]
      val repositoryDispatcher = getMockExecutionContext
      val mongoRepositoryMock = mock[MongoDbService]
      val redisCacheServiceMock = mock[RedisCacheService]

      val appConfigMock = mock[AppConfigService]
      when(appConfigMock.properties) thenReturn testConfigProperties

      val service = new MarketplaceRepositoryImpl(appConfigMock, walmartMock, bestbuyMock, ebayMock, amazonMock, mongoRepositoryMock,
        redisCacheServiceMock)(repositoryDispatcher)

      val response = service.getProductDetail("1", "", BestBuy)

      whenReady(response, timeout(defaultTestAsyncAwaitTimeout seconds), interval(defaultTestAsyncInterval millis)) {
        r: Option[OfferDetail] =>
          r mustBe None

          // verify external sources not called
          verify(bestbuyMock, times(0)).getProductDetail(anyString, anyString, anyString)
          verify(walmartMock, times(0)).getProductDetail(anyString, anyString, anyString)
          verify(ebayMock, times(0)).getProductDetail(anyString, anyString, anyString)
          verify(amazonMock, times(0)).getProductDetail(anyString, anyString, anyString)
      }
    }

    "be empty when empty Source is sent as param" in {
      val bestbuyMock = mock[BestBuyRepository]
      val walmartMock = mock[WalmartRepository]
      val ebayMock = mock[EbayRepository]
      val amazonMock = mock[AmazonRepository]
      val repositoryDispatcher = getMockExecutionContext
      val mongoRepositoryMock = mock[MongoDbService]
      val redisCacheServiceMock = mock[RedisCacheService]

      val appConfigMock = mock[AppConfigService]
      when(appConfigMock.properties) thenReturn testConfigProperties

      val service = new MarketplaceRepositoryImpl(appConfigMock, walmartMock, bestbuyMock, ebayMock,
        amazonMock, mongoRepositoryMock, redisCacheServiceMock)(repositoryDispatcher)

      val response = service.getProductDetail("1", Id, "")

      whenReady(response, timeout(defaultTestAsyncAwaitTimeout seconds), interval(defaultTestAsyncInterval millis)) {
        r: Option[OfferDetail] =>
          r mustBe None

          // verify external sources not called
          verify(bestbuyMock, times(0)).getProductDetail(anyString, anyString, anyString)
          verify(walmartMock, times(0)).getProductDetail(anyString, anyString, anyString)
          verify(ebayMock, times(0)).getProductDetail(anyString, anyString, anyString)
          verify(amazonMock, times(0)).getProductDetail(anyString, anyString, anyString)
      }
    }

    "be empty when invalid IdType is sent as param" in {
      val bestbuyMock = mock[BestBuyRepository]
      val walmartMock = mock[WalmartRepository]
      val ebayMock = mock[EbayRepository]
      val amazonMock = mock[AmazonRepository]
      val repositoryDispatcher = getMockExecutionContext
      val mongoRepositoryMock = mock[MongoDbService]
      val redisCacheServiceMock = mock[RedisCacheService]

      val appConfigMock = mock[AppConfigService]
      when(appConfigMock.properties) thenReturn testConfigProperties

      val service = new MarketplaceRepositoryImpl(appConfigMock, walmartMock, bestbuyMock,
        ebayMock, amazonMock, mongoRepositoryMock, redisCacheServiceMock)(repositoryDispatcher)

      val response = service.getProductDetail("1", "XYZ", BestBuy)

      whenReady(response, timeout(defaultTestAsyncAwaitTimeout seconds), interval(defaultTestAsyncInterval millis)) {
        r: Option[OfferDetail] =>
          r mustBe None

          // verify external sources not called
          verify(bestbuyMock, times(0)).getProductDetail(anyString, anyString, anyString)
          verify(walmartMock, times(0)).getProductDetail(anyString, anyString, anyString)
          verify(ebayMock, times(0)).getProductDetail(anyString, anyString, anyString)
          verify(amazonMock, times(0)).getProductDetail(anyString, anyString, anyString)
      }
    }

    "be empty when invalid Source is sent as param" in {
      val bestbuyMock = mock[BestBuyRepository]
      val walmartMock = mock[WalmartRepository]
      val ebayMock = mock[EbayRepository]
      val amazonMock = mock[AmazonRepository]
      val repositoryDispatcher = getMockExecutionContext
      val mongoRepositoryMock = mock[MongoDbService]
      val redisCacheServiceMock = mock[RedisCacheService]

      val appConfigMock = mock[AppConfigService]
      when(appConfigMock.properties) thenReturn testConfigProperties

      val service = new MarketplaceRepositoryImpl(appConfigMock, walmartMock,
        bestbuyMock, ebayMock, amazonMock, mongoRepositoryMock, redisCacheServiceMock)(repositoryDispatcher)

      val response = service.getProductDetail("1", Id, "XYZ")

      whenReady(response, timeout(defaultTestAsyncAwaitTimeout seconds), interval(defaultTestAsyncInterval millis)) {
        r: Option[OfferDetail] =>
          r mustBe None

          // verify external sources not called
          verify(bestbuyMock, times(0)).getProductDetail(anyString, anyString, anyString)
          verify(walmartMock, times(0)).getProductDetail(anyString, anyString, anyString)
          verify(ebayMock, times(0)).getProductDetail(anyString, anyString, anyString)
          verify(amazonMock, times(0)).getProductDetail(anyString, anyString, anyString)
      }
    }
  }

}

package app.product.marketplace.ebay

import common.BaseFunSuiteDomainTest
import common.MockBaseUtil._
import common.config.AppConfigService
import mockws.MockWS
import org.junit.runner.RunWith
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatest.junit.JUnitRunner
import play.api.libs.json.Json
import play.api.mvc.Results._
import play.api.test.Helpers._
import product.marketplace.common.MarketplaceConstants._
import product.marketplace.common.RequestMonitor
import product.marketplace.ebay.EbayRepositoryImpl
import product.model.ListRequest

import scala.io.Source

@RunWith(classOf[JUnitRunner])
class EbayRepositorySpec extends BaseFunSuiteDomainTest {

  val MockPath = s"$MockMarketplaceFilesPath/ebay"

  test("search should produce a valid JSON") {
    val ws = MockWS {
      case (GET, "http://svcs.ebay.com/services/search/FindingService/v1") => Action {
        Ok(Json.parse(Source.fromFile(s"$MockPath/ebay_sample_search_response.json").getLines.mkString))
      }
    }
    val appConfigMock = mock[AppConfigService]
    when(appConfigMock.properties) thenReturn testConfigProperties
    when(appConfigMock.buildImgUrl(Some(any[String]))) thenReturn "http://localhost:5555/assets/images/ebay-logo.png"
    when(appConfigMock.buildImgUrlExternal(Some(any[String]), any[Boolean])) thenReturn "http://thumbs4.ebaystatic.com/m/m49zp0OjDqyXfk9jImQwApg/140.jpg"

    val requestMonitorMock = mock[RequestMonitor]
    when(requestMonitorMock.isRequestPossible(Ebay)) thenReturn true

    val repositoryDispatcher = getMockWorkerExecutionContext

    val service = new EbayRepositoryImpl(ws, appConfigMock, requestMonitorMock)(repositoryDispatcher)

    val params = ListRequest.fromKeyword(ListRequest(), "skyrim")
    val result = await(service.search(params)).get

    result.summary.page shouldEqual 1
    result.summary.pageCount shouldEqual 660
    result.summary.totalCount shouldEqual 6591
    result.list.head.id shouldEqual "263005367951"
    result.list.head.upc shouldEqual None
    result.list.head.name shouldEqual "The Elder Scrolls V: Skyrim Special Edition PS4 [Factory Refurbished]"
    result.list.head.partyName shouldEqual "ebay.com"
    result.list.head.mainImageFileUrl shouldEqual "http://thumbs4.ebaystatic.com/m/m49zp0OjDqyXfk9jImQwApg/140.jpg"
    result.list.head.partyImageFileUrl shouldEqual "http://localhost:5555/assets/images/ebay-logo.png"
    result.list.head.productCategory shouldEqual "Video Games"
    result.list.head.price shouldEqual 25.67d
    result.list.head.numReviews shouldEqual 0
    result.list.head.rating shouldEqual 0.0f
  }

  test("search with no params should return Random search term from config file") {
    val ws = MockWS {
      case (GET, "http://svcs.ebay.com/services/search/FindingService/v1") => Action {
        Ok(Json.parse(Source.fromFile(s"$MockPath/ebay_sample_search_no_keyword.json").getLines.mkString))
      }
    }
    val appConfigMock = mock[AppConfigService]
    when(appConfigMock.properties) thenReturn testConfigProperties
    when(appConfigMock.buildImgUrl(Some(any[String]))) thenReturn "http://localhost:5555/assets/images/ebay-logo.png"
    when(appConfigMock.buildImgUrlExternal(Some(any[String]), any[Boolean])) thenReturn "http://thumbs4.ebaystatic.com/m/m49zp0OjDqyXfk9jImQwApg/140.jpg"

    val requestMonitorMock = mock[RequestMonitor]
    when(requestMonitorMock.isRequestPossible(Ebay)) thenReturn true

    val repositoryDispatcher = getMockWorkerExecutionContext

    val service = new EbayRepositoryImpl(ws, appConfigMock, requestMonitorMock)(repositoryDispatcher)
    val result = await(service.search(ListRequest())).get

    result.summary.page shouldEqual 1
    result.summary.pageCount shouldEqual 3217390
    result.summary.totalCount shouldEqual 32173895
    result.list.head.id shouldEqual "202018383733"
    result.list.head.upc shouldEqual None
    result.list.head.name shouldEqual "Adidas F99532 Original VS Hoops Mid Shoes"
    result.list.head.semanticName shouldEqual "http://rover.ebay.com/rover/1/711-53200-19255-0/1?ff3=2&toolid=10041&campid=test12345678"
    result.list.head.partyName shouldEqual "ebay.com"
    result.list.head.mainImageFileUrl shouldEqual "http://thumbs4.ebaystatic.com/m/m49zp0OjDqyXfk9jImQwApg/140.jpg"
    result.list.head.partyImageFileUrl shouldEqual "http://localhost:5555/assets/images/ebay-logo.png"
    result.list.head.productCategory shouldEqual "Athletic"
    result.list.head.price shouldEqual 38.0d
    result.list.head.numReviews shouldEqual 0
    result.list.head.rating shouldEqual 0.0f
  }

  test("search by keyword no results should return None") {
    val ws = MockWS {
      case (GET, "http://svcs.ebay.com/services/search/FindingService/v1") => Action {
        Ok(Json.parse(Source.fromFile(s"$MockPath/ebay_search_no_results.json").getLines.mkString))
      }
    }
    val appConfigMock = mock[AppConfigService]
    when(appConfigMock.properties) thenReturn testConfigProperties

    val requestMonitorMock = mock[RequestMonitor]
    when(requestMonitorMock.isRequestPossible(Ebay)) thenReturn true

    val repositoryDispatcher = getMockWorkerExecutionContext

    val service = new EbayRepositoryImpl(ws, appConfigMock, requestMonitorMock)(repositoryDispatcher)

    val params = ListRequest.fromKeyword(ListRequest(), "xxxccasd")
    val result = await(service.search(params))
    result shouldBe None
  }

  test("search by keyword when requestMonitor is Busy expect Future successful None") {
    val ws = MockWS {
      case (GET, "http://svcs.ebay.com/services/search/FindingService/v1") => Action { Ok }
    }
    val appConfigMock = mock[AppConfigService]
    when(appConfigMock.properties) thenReturn testConfigProperties

    val requestMonitorMock = mock[RequestMonitor]
    when(requestMonitorMock.isRequestPossible(Ebay)) thenReturn false

    val repositoryDispatcher = getMockWorkerExecutionContext

    val service = new EbayRepositoryImpl(ws, appConfigMock, requestMonitorMock)(repositoryDispatcher)

    val params = ListRequest.fromKeyword(ListRequest(), "prod123")
    val result = await(service.search(params))
    result shouldBe None
  }

  test("getProductDetail by Id should produce a valid JSON") {
    val ws = MockWS {
      case (GET, "http://svcs.ebay.com/services/search/FindingService/v1") => Action {
        Ok(Json.parse(Source.fromFile(s"$MockPath/ebay_sample_product_detail_response.json").getLines.mkString))
      }
    }

    val appConfigMock = mock[AppConfigService]
    when(appConfigMock.properties) thenReturn testConfigProperties
    when(appConfigMock.buildImgUrl(Some(any[String]))) thenReturn "http://localhost:5555/assets/images/ebay-logo.png"
    when(appConfigMock.buildImgUrlExternal(Some(any[String]), any[Boolean])) thenReturn "http://thumbs4.ebaystatic.com/m/m49zp0OjDqyXfk9jImQwApg/140.jpg"

    val requestMonitorMock = mock[RequestMonitor]
    when(requestMonitorMock.isRequestPossible(Ebay)) thenReturn true

    val repositoryDispatcher = getMockWorkerExecutionContext

    val service = new EbayRepositoryImpl(ws, appConfigMock, requestMonitorMock)(repositoryDispatcher)

    val result = await(service.getProductDetail("62923188", Id, Ebay, Some(UnitedStates))).get

    result.offer.id shouldEqual "62923188"
    result.offer.upc shouldEqual None
    result.offer.name shouldEqual "Harry Potter and the Order of the Phoenix-(DVD, Widescreen Edition)BRAND NEW!!!"
    result.offer.semanticName shouldEqual "http://www.ebay.com/itm/Harry-Potter-and-Order-Phoenix-DVD-Widescreen-Edition-BRAND-NEW-/232135853334"
    result.offer.partyName shouldEqual "ebay.com"
    result.offer.mainImageFileUrl shouldEqual "http://thumbs4.ebaystatic.com/m/m49zp0OjDqyXfk9jImQwApg/140.jpg"
    result.offer.partyImageFileUrl shouldEqual "http://localhost:5555/assets/images/ebay-logo.png"
    result.offer.productCategory shouldEqual "DVDs & Blu-ray Discs"
    result.offer.price shouldEqual 5.62
    result.offer.numReviews shouldEqual 0
    result.offer.rating shouldEqual 0
  }

  test("getProductDetail by UPC should produce a valid JSON") {
    val ws = MockWS {
      case (GET, "http://svcs.ebay.com/services/search/FindingService/v1") => Action {
        Ok(Json.parse(Source.fromFile(s"$MockPath/ebay_find_by_upc.json").getLines.mkString))
      }
    }

    val appConfigMock = mock[AppConfigService]
    when(appConfigMock.properties) thenReturn testConfigProperties
    when(appConfigMock.buildImgUrl(Some(any[String]))) thenReturn "http://localhost:5555/assets/images/ebay-logo.png"
    when(appConfigMock.buildImgUrlExternal(Some(any[String]), any[Boolean])) thenReturn "http://thumbs4.ebaystatic.com/m/m49zp0OjDqyXfk9jImQwApg/140.jpg"

    val requestMonitorMock = mock[RequestMonitor]
    when(requestMonitorMock.isRequestPossible(Ebay)) thenReturn true

    val repositoryDispatcher = getMockWorkerExecutionContext

    val service = new EbayRepositoryImpl(ws, appConfigMock, requestMonitorMock)(repositoryDispatcher)

    val result = await(service.getProductDetail("0883974187416", Upc, Ebay, Some(UnitedStates))).get

    result.offer.id shouldEqual "129872399"
    result.offer.upc shouldEqual None
    result.offer.name shouldEqual "New Laptop Toshiba Satellite L355-S7907 17\" Intel Pentium Dual-core T3400(2.16Gh"
    result.offer.semanticName shouldEqual "http://www.ebay.com/itm/New-Laptop-Toshiba-Satellite-L355-S7907-17-Intel-Pentium-Dual-core-T3400-2-16Gh-/302480607070"
    result.offer.partyName shouldEqual "ebay.com"
    result.offer.mainImageFileUrl shouldEqual "http://thumbs4.ebaystatic.com/m/m49zp0OjDqyXfk9jImQwApg/140.jpg"
    result.offer.partyImageFileUrl shouldEqual "http://localhost:5555/assets/images/ebay-logo.png"
    result.offer.productCategory shouldEqual "PC Laptops & Netbooks"
    result.offer.price shouldEqual 1.04
    result.offer.numReviews shouldEqual 0
    result.offer.rating shouldEqual 0
  }

  test("getProductDetail using invalid id should return None") {
    val ws = MockWS {
      case (GET, "http://svcs.ebay.com/services/search/FindingService/v1") => Action {
        Ok(Json.parse(Source.fromFile(s"$MockPath/ebay_invalid_product_id_response.json").getLines.mkString))
      }
    }

    val appConfigMock = mock[AppConfigService]
    when(appConfigMock.properties) thenReturn testConfigProperties

    val requestMonitorMock = mock[RequestMonitor]
    when(requestMonitorMock.isRequestPossible(Ebay)) thenReturn true

    val repositoryDispatcher = getMockWorkerExecutionContext

    val service = new EbayRepositoryImpl(ws, appConfigMock, requestMonitorMock)(repositoryDispatcher)

    val result = await(service.getProductDetail("abcxyz", Id, Ebay, Some(UnitedStates)))
    result shouldBe None
  }

  test("getProductDetail using invalid idType should return None") {
    val ws = MockWS {
      case (GET, "http://svcs.ebay.com/services/search/FindingService/v1") => Action {
        Ok(Json.parse(Source.fromFile(s"$MockPath/ebay_sample_product_detail_response.json").getLines.mkString))
      }
    }

    val appConfigMock = mock[AppConfigService]
    when(appConfigMock.properties) thenReturn testConfigProperties

    val requestMonitorMock = mock[RequestMonitor]
    when(requestMonitorMock.isRequestPossible(Ebay)) thenReturn true

    val repositoryDispatcher = getMockWorkerExecutionContext

    val service = new EbayRepositoryImpl(ws, appConfigMock, requestMonitorMock)(repositoryDispatcher)

    val result = await(service.getProductDetail("62923188", "XYZ", Ebay, Some(UnitedStates)))
    result shouldBe None
  }

  test("getProductDetail by Id no results should return None") {
    val ws = MockWS {
      case (GET, "http://svcs.ebay.com/services/search/FindingService/v1") => Action {
        Ok(Json.parse(Source.fromFile(s"$MockPath/ebay_find_by_id_no_result.json").getLines.mkString))
      }
    }

    val appConfigMock = mock[AppConfigService]
    when(appConfigMock.properties) thenReturn testConfigProperties

    val requestMonitorMock = mock[RequestMonitor]
    when(requestMonitorMock.isRequestPossible(Ebay)) thenReturn true

    val repositoryDispatcher = getMockWorkerExecutionContext

    val service = new EbayRepositoryImpl(ws, appConfigMock, requestMonitorMock)(repositoryDispatcher)

    val result = await(service.getProductDetail("32328", Id, Ebay, Some(UnitedStates)))
    result shouldBe None
  }

  test("getProductDetail by Upc no results should return None") {
    val ws = MockWS {
      case (GET, "http://svcs.ebay.com/services/search/FindingService/v1") => Action {
        Ok(Json.parse(Source.fromFile(s"$MockPath/ebay_find_by_upc_no_result.json").getLines.mkString))
      }
    }

    val appConfigMock = mock[AppConfigService]
    when(appConfigMock.properties) thenReturn testConfigProperties

    val requestMonitorMock = mock[RequestMonitor]
    when(requestMonitorMock.isRequestPossible(Ebay)) thenReturn true

    val repositoryDispatcher = getMockWorkerExecutionContext

    val service = new EbayRepositoryImpl(ws, appConfigMock, requestMonitorMock)(repositoryDispatcher)

    val result = await(service.getProductDetail("088374187416", Upc, Ebay, Some(UnitedStates)))
    result shouldBe None
  }

  test("getProductDetail when requestMonitor is Busy expect Future successful None") {
    val ws = MockWS {
      case (GET, "http://svcs.ebay.com/services/search/FindingService/v1") => Action { Ok }
    }

    val appConfigMock = mock[AppConfigService]
    when(appConfigMock.properties) thenReturn testConfigProperties

    val requestMonitorMock = mock[RequestMonitor]
    when(requestMonitorMock.isRequestPossible(Ebay)) thenReturn false

    val repositoryDispatcher = getMockWorkerExecutionContext

    val service = new EbayRepositoryImpl(ws, appConfigMock, requestMonitorMock)(repositoryDispatcher)

    val result = await(service.getProductDetail("62923188", Id, Ebay, Some(UnitedStates)))
    result shouldBe None
  }
}
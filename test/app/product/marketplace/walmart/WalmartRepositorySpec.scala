package app.product.marketplace.walmart

import common.BaseFunSuiteDomainTest
import common.MockBaseUtil._
import common.config.AppConfigService
import common.monitor.RequestMonitor
import mockws.MockWS
import org.junit.runner.RunWith
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatest.junit.JUnitRunner
import play.api.libs.json.Json
import play.api.mvc.Results._
import play.api.test.Helpers._
import product.marketplace.common.MarketplaceConstants._
import product.marketplace.walmart.WalmartRepositoryImpl
import product.model.{ListRequest, OfferDetail}

import scala.io.Source

@RunWith(classOf[JUnitRunner])
class WalmartRepositorySpec extends BaseFunSuiteDomainTest {

  val MockPath = s"$MockMarketplaceFilesPath/walmart"

  private def validateProductDetail(result : OfferDetail) = {
    result.offer.id shouldEqual "55760264"
    result.offer.upc shouldEqual Some("065857174434")
    result.offer.name shouldEqual "Better Homes and Gardens Leighton Twin-Over-Full Bunk Bed, Multiple Colors"
    result.offer.semanticName shouldEqual "http://linksynergy.walmart.com/fs-bin/click?id=12345678&offerid=223073"
    result.offer.partyName shouldEqual "walmart.com"
    result.offer.mainImageFileUrl shouldEqual "https://i5.walmartimages.com/asr/4769fa9e-2206-4840-a7d7-a513e82df468_1.jpeg"
    result.offer.partyImageFileUrl shouldEqual "http://localhost:5555/assets/images/walmart-logo.png"
    result.offer.productCategory shouldEqual "Home/Kids' Rooms/Kids' Furniture/Kids' Bunk Beds"
    result.offer.price shouldEqual 275.0d
    result.offer.numReviews shouldEqual 0
    result.offer.rating shouldEqual 4.393f
  }

  test("search by keyword should produce a valid JSON") {
    val ws = MockWS {
      case (GET, "http://api.walmartlabs.com/v1/search") => Action {
        Ok(Json.parse(Source.fromFile(s"$MockPath/walmart_sample_search_response.json").getLines.mkString))
      }
    }

    val appConfigMock = mock[AppConfigService]
    when(appConfigMock.properties) thenReturn testConfigProperties
    when(appConfigMock.buildImgUrl(Some(any[String]))) thenReturn "http://localhost:5555/assets/images/walmart-logo.png"
    when(appConfigMock.buildImgUrlExternal(Some(any[String]), any[Boolean])) thenReturn "https://i5.walmartimages.com/asr/4769fa9e-2206-4840-a7d7-a513e82df468_1.jpeg"

    val requestMonitorMock = mock[RequestMonitor]
    when(requestMonitorMock.isRequestPossible(Walmart)) thenReturn true

    val repositoryDispatcher = getMockWorkerExecutionContext

    val service = new WalmartRepositoryImpl(ws, appConfigMock, requestMonitorMock)(repositoryDispatcher)

    val params = ListRequest.fromKeyword(ListRequest(), "skyrim")
    val result = await(service.search(params)).get

    result.summary.page shouldEqual 1
    result.summary.pageCount shouldEqual 3
    result.summary.totalCount shouldEqual 38
    result.list.head.id shouldEqual "53966162"
    result.list.head.upc shouldEqual Some("093155171244")
    result.list.head.name shouldEqual "Skyrim Special Edition (Xbox One)"
    result.list.head.partyName shouldEqual "walmart.com"
    result.list.head.mainImageFileUrl shouldEqual "https://i5.walmartimages.com/asr/4769fa9e-2206-4840-a7d7-a513e82df468_1.jpeg"
    result.list.head.partyImageFileUrl shouldEqual "http://localhost:5555/assets/images/walmart-logo.png"
    result.list.head.productCategory shouldEqual "Video Games/Xbox One Consoles, Games & Accessories/Xbox One Games"
    result.list.head.price shouldEqual 30.74d
    result.list.head.numReviews shouldEqual 24
    result.list.head.rating shouldEqual 4.87f
  }

  test("search by keyword no results should return None") {
    val ws = MockWS {
      case (GET, "http://api.walmartlabs.com/v1/search") => Action {
        Ok(Json.parse(Source.fromFile(s"$MockPath/walmart_sample_search_no_results.json").getLines.mkString))
      }
    }

    val appConfigMock = mock[AppConfigService]
    when(appConfigMock.properties) thenReturn testConfigProperties

    val requestMonitorMock = mock[RequestMonitor]
    when(requestMonitorMock.isRequestPossible(Walmart)) thenReturn true

    val repositoryDispatcher = getMockWorkerExecutionContext

    val service = new WalmartRepositoryImpl(ws, appConfigMock, requestMonitorMock)(repositoryDispatcher)

    val params = ListRequest.fromKeyword(ListRequest(), "sk12321323")
    val result = await(service.search(params))
    result shouldBe None
  }

  test("search with no params should return Trending List result") {
    val ws = MockWS {
      case (GET, "http://api.walmartlabs.com/v1/trends") => Action {
        Ok(Json.parse(Source.fromFile(s"$MockPath/walmart_sample_trend_response.json").getLines.mkString))
      }
    }

    val appConfigMock = mock[AppConfigService]
    when(appConfigMock.properties) thenReturn testConfigProperties
    when(appConfigMock.buildImgUrl(Some(any[String]))) thenReturn "http://localhost:5555/assets/images/walmart-logo.png"
    when(appConfigMock.buildImgUrlExternal(Some(any[String]), any[Boolean])) thenReturn "https://i5.walmartimages.com/asr/4769fa9e-2206-4840-a7d7-a513e82df468_1.jpeg"

    val requestMonitorMock = mock[RequestMonitor]
    when(requestMonitorMock.isRequestPossible(Walmart)) thenReturn true

    val repositoryDispatcher = getMockWorkerExecutionContext

    val service = new WalmartRepositoryImpl(ws, appConfigMock, requestMonitorMock)(repositoryDispatcher)

    val result = await(service.search(ListRequest())).get

    result.summary.page shouldEqual 1
    result.summary.pageCount shouldEqual 2
    result.summary.totalCount shouldEqual 25
    result.list.head.id shouldEqual "55760264"
    result.list.head.upc shouldEqual Some("065857174434")
    result.list.head.name shouldEqual "Better Homes and Gardens Leighton Twin-Over-Full Bunk Bed, Multiple Colors"
    result.list.head.partyName shouldEqual "walmart.com"
    result.list.head.mainImageFileUrl shouldEqual "https://i5.walmartimages.com/asr/4769fa9e-2206-4840-a7d7-a513e82df468_1.jpeg"
    result.list.head.partyImageFileUrl shouldEqual "http://localhost:5555/assets/images/walmart-logo.png"
    result.list.head.productCategory shouldEqual "Home/Kids' Rooms/Kids' Furniture/Kids' Bunk Beds"
    result.list.head.price shouldEqual 275.0d
    result.list.head.numReviews shouldEqual 0
    result.list.head.rating shouldEqual 4.413f
  }

  test("search by keyword when requestMonitor is Busy expect Future successful None") {
    val ws = MockWS {
      case (GET, "http://api.walmartlabs.com/v1/search") => Action { Ok }
    }

    val appConfigMock = mock[AppConfigService]
    when(appConfigMock.properties) thenReturn testConfigProperties

    val requestMonitorMock = mock[RequestMonitor]
    when(requestMonitorMock.isRequestPossible(Walmart)) thenReturn false

    val repositoryDispatcher = getMockWorkerExecutionContext

    val service = new WalmartRepositoryImpl(ws, appConfigMock, requestMonitorMock)(repositoryDispatcher)
    val params = ListRequest.fromKeyword(ListRequest(), "prod123")
    val result = await(service.search(params))
    result shouldBe None
  }

  test("getProductDetail by Id should produce a valid JSON") {
    val ws = MockWS {
      case (GET, "http://api.walmartlabs.com/v1/items/55760264") => Action {
        Ok(Json.parse(Source.fromFile(s"$MockPath/walmart_sample_product_detail_response.json").getLines.mkString))
      }
    }

    val appConfigMock = mock[AppConfigService]
    when(appConfigMock.properties) thenReturn testConfigProperties
    when(appConfigMock.buildImgUrl(Some(any[String]))) thenReturn "http://localhost:5555/assets/images/walmart-logo.png"
    when(appConfigMock.buildImgUrlExternal(Some(any[String]), any[Boolean])) thenReturn "https://i5.walmartimages.com/asr/4769fa9e-2206-4840-a7d7-a513e82df468_1.jpeg"

    val requestMonitorMock = mock[RequestMonitor]
    when(requestMonitorMock.isRequestPossible(Walmart)) thenReturn true

    val repositoryDispatcher = getMockWorkerExecutionContext

    val service = new WalmartRepositoryImpl(ws, appConfigMock, requestMonitorMock)(repositoryDispatcher)

    val result = await(service.getProductDetail("55760264", Id, Walmart)).get

    validateProductDetail(result)
  }

  test("getProductDetail by Id not found should return None") {
    val ws = MockWS {
      case (GET, "http://api.walmartlabs.com/v1/items/12345") => Action {
        Ok(Json.parse(Source.fromFile(s"$MockPath/walmart_get_by_id_invalid.json").getLines.mkString))
      }
    }

    val appConfigMock = mock[AppConfigService]
    when(appConfigMock.properties) thenReturn testConfigProperties

    val requestMonitorMock = mock[RequestMonitor]
    when(requestMonitorMock.isRequestPossible(Walmart)) thenReturn true

    val repositoryDispatcher = getMockWorkerExecutionContext

    val service = new WalmartRepositoryImpl(ws, appConfigMock, requestMonitorMock)(repositoryDispatcher)

    val result = await(service.getProductDetail("12345", Id, Walmart))
    result shouldBe None
  }

  test("getProductDetail by UPC should produce a valid JSON") {
    val ws = MockWS {
      case (GET, "http://api.walmartlabs.com/v1/items") => Action {
        Ok(Json.parse(Source.fromFile(s"$MockPath/walmart_sample_upc_prod_detail_response.json").getLines.mkString))
      }
    }

    val appConfigMock = mock[AppConfigService]
    when(appConfigMock.properties) thenReturn testConfigProperties
    when(appConfigMock.buildImgUrl(Some(any[String]))) thenReturn "http://localhost:5555/assets/images/walmart-logo.png"
    when(appConfigMock.buildImgUrlExternal(Some(any[String]), any[Boolean])) thenReturn "https://i5.walmartimages.com/asr/4769fa9e-2206-4840-a7d7-a513e82df468_1.jpeg"

    val requestMonitorMock = mock[RequestMonitor]
    when(requestMonitorMock.isRequestPossible(Walmart)) thenReturn true

    val repositoryDispatcher = getMockWorkerExecutionContext

    val service = new WalmartRepositoryImpl(ws, appConfigMock, requestMonitorMock)(repositoryDispatcher)

    val result = await(service.getProductDetail("065857174434", Upc, Walmart)).get

    validateProductDetail(result)
  }

  test("getProductDetail by UPC not found should return None") {
    val ws = MockWS {
      case (GET, "http://api.walmartlabs.com/v1/items") => Action {
        Ok(Json.parse(Source.fromFile(s"$MockPath/walmart_get_by_upc_not_found.json").getLines.mkString))
      }
    }

    val appConfigMock = mock[AppConfigService]
    when(appConfigMock.properties) thenReturn testConfigProperties

    val requestMonitorMock = mock[RequestMonitor]
    when(requestMonitorMock.isRequestPossible(Walmart)) thenReturn true

    val repositoryDispatcher = getMockWorkerExecutionContext

    val service = new WalmartRepositoryImpl(ws, appConfigMock, requestMonitorMock)(repositoryDispatcher)

    val result = await(service.getProductDetail("12345", Upc, Walmart))
    result shouldBe None
  }

  test("getProductDetail using invalid idType should return None") {
    val ws = MockWS {
      case (GET, "http://api.walmartlabs.com/v1/items") => Action { Ok }
    }

    val appConfigMock = mock[AppConfigService]
    when(appConfigMock.properties) thenReturn testConfigProperties

    val requestMonitorMock = mock[RequestMonitor]
    when(requestMonitorMock.isRequestPossible(Walmart)) thenReturn true

    val repositoryDispatcher = getMockWorkerExecutionContext

    val service = new WalmartRepositoryImpl(ws, appConfigMock, requestMonitorMock)(repositoryDispatcher)
    val result = await(service.getProductDetail("55760264", "XYZ", Walmart))

    result shouldBe None
  }

  test("getProductDetail when requestMonitor is Busy expect Future successful None") {
    val ws = MockWS {
      case (GET, "http://svcs.ebay.com/services/search/FindingService/v1") => Action { Ok }
    }

    val appConfigMock = mock[AppConfigService]
    when(appConfigMock.properties) thenReturn testConfigProperties

    val requestMonitorMock = mock[RequestMonitor]
    when(requestMonitorMock.isRequestPossible(Walmart)) thenReturn false

    val repositoryDispatcher = getMockWorkerExecutionContext

    val service = new WalmartRepositoryImpl(ws, appConfigMock, requestMonitorMock)(repositoryDispatcher)

    val result = await(service.getProductDetail("55760264", Id, Walmart))
    result shouldBe None
  }
}
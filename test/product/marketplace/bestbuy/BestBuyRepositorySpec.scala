package app.product.marketplace.bestbuy

import app.product.marketplace.common.MarketplaceConstants._
import app.product.marketplace.common.RequestMonitor
import app.product.model.OfferDetail
import common.config.AppConfigService
import mockws.MockWS
import org.junit.runner.RunWith
import org.mockito.Matchers._
import org.mockito.Mockito.when
import org.scalatest.junit.JUnitRunner
import org.scalatest.mockito.MockitoSugar
import org.scalatest.prop.PropertyChecks
import org.scalatest.{FunSuite, Matchers}
import play.api.libs.json.Json
import play.api.mvc.Results._
import play.api.test.Helpers._
import product.util.MockBaseUtil._

import scala.io.Source

@RunWith(classOf[JUnitRunner])
class BestBuyRepositorySpec extends FunSuite with Matchers with PropertyChecks with MockitoSugar {

  val MockPath = s"$MockMarketplaceFilesPath/bestbuy"

  private def validateProductDetail(result : OfferDetail) = {
    result.offer.id shouldEqual "5529006"
    result.offer.upc shouldEqual Some("849803052423")
    result.offer.name shouldEqual "Funko - Elder Scrolls V: Skyrim Dovahkiin Pop! Vinyl Figure"
    result.offer.semanticName shouldEqual "https://api.bestbuy.com/click/-/5529006/pdp"
    result.offer.partyName shouldEqual "bestbuy.com"
    result.offer.mainImageFileUrl shouldEqual "https://img.bbystatic.com/BestBuy_US/images/products/5529/5529006_sa.jpg"
    result.offer.partyImageFileUrl shouldEqual "http://localhost:5555/assets/images/best-buy-logo.png"
    result.offer.productCategory shouldEqual "Best Buy-Drones, Toys & Collectibles-Licensed Collectibles-Video Game Collectibles"
    result.offer.price shouldEqual 9.99
    result.offer.numReviews shouldEqual 28
    result.offer.rating shouldEqual 4.8f
  }

  test("search should produce a valid JSON") {
    val ws = MockWS {
      case (GET, "https://api.bestbuy.com/v1/products(search=skyrim)") => actionBuilder {
        Ok(Json.parse(Source.fromFile(s"$MockPath/best_buy_sample_search_response.json").getLines.mkString))
      }
    }

    val appConfigMock = mock[AppConfigService]
    when(appConfigMock.properties) thenReturn testConfigProperties
    when(appConfigMock.buildImgUrl(Some(any[String]))) thenReturn "http://localhost:5555/assets/images/best-buy-logo.png"
    when(appConfigMock.buildImgUrlExternal(Some(any[String]), any[Boolean])) thenReturn "https://img.bbystatic.com/BestBuy_US/images/products/5529/5529006_sa.jpg"

    val requestMonitorMock = mock[RequestMonitor]
    when(requestMonitorMock.isRequestPossible(BestBuy)) thenReturn true

    val repositoryDispatcher = getMockWorkerExecutionContext

    val service = new BestBuyRepositoryImpl(ws, appConfigMock, requestMonitorMock)(repositoryDispatcher)

    val p : Map[String,String] = Map(
      Name -> "skyrim",
      Page -> "1"
    )

    val result = await(service.search(p)).get

    result.summary.page shouldEqual 1
    result.summary.pageCount shouldEqual 2
    result.summary.totalCount shouldEqual 18
    result.list.head.id shouldEqual "5529006"
    result.list.head.upc shouldEqual Some("849803052423")
    result.list.head.name shouldEqual "Funko - Elder Scrolls V: Skyrim Dovahkiin Pop! Vinyl Figure - Multi"
    result.list.head.semanticName shouldEqual "https://api.bestbuy.com/click/-/5529006/pdp"
    result.list.head.partyName shouldEqual "bestbuy.com"
    result.list.head.mainImageFileUrl shouldEqual "https://img.bbystatic.com/BestBuy_US/images/products/5529/5529006_sa.jpg"
    result.list.head.partyImageFileUrl shouldEqual "http://localhost:5555/assets/images/best-buy-logo.png"
    result.list.head.productCategory shouldEqual "Best Buy-Drones, Toys & Collectibles-Licensed Collectibles-Video Game Collectibles"
    result.list.head.price shouldEqual 9.99d
    result.list.head.numReviews shouldEqual 28
    result.list.head.rating shouldEqual 4.8f
  }

  test("search with no params should return Trending search response") {
    val ws = MockWS {
      case (GET, "https://api.bestbuy.com/beta/products/trendingViewed") => actionBuilder {
        Ok(Json.parse(Source.fromFile(s"$MockPath/best_buy_sample_trending_response.json").getLines.mkString))
      }
    }

    val appConfigMock = mock[AppConfigService]
    when(appConfigMock.properties) thenReturn testConfigProperties
    when(appConfigMock.buildImgUrl(Some(any[String]))) thenReturn "http://localhost:5555/assets/images/best-buy-logo.png"
    when(appConfigMock.buildImgUrlExternal(Some(any[String]), any[Boolean])) thenReturn "https://img.bbystatic.com/BestBuy_US/images/products/5529/5529006_sa.jpg"

    val requestMonitorMock = mock[RequestMonitor]
    when(requestMonitorMock.isRequestPossible(BestBuy)) thenReturn true

    val repositoryDispatcher = getMockWorkerExecutionContext

    val service = new BestBuyRepositoryImpl(ws, appConfigMock, requestMonitorMock)(repositoryDispatcher)

    val p : Map[String,String] = Map()
    val result = await(service.search(p)).get

    result.summary.page shouldEqual 1
    result.summary.pageCount shouldEqual 1
    result.summary.totalCount shouldEqual 10
    result.list.head.id shouldEqual "5789803"
    result.list.head.upc shouldEqual None
    result.list.head.name shouldEqual "Samsung - 40\" Class (39.5\" Diag.) - LED - 2160p - Smart - 4K Ultra HD TV with High Dynamic Range"
    result.list.head.semanticName shouldEqual "https://api.bestbuy.com/click/-/5789803/pdp"
    result.list.head.partyName shouldEqual "bestbuy.com"
    result.list.head.mainImageFileUrl shouldEqual "https://img.bbystatic.com/BestBuy_US/images/products/5529/5529006_sa.jpg"
    result.list.head.partyImageFileUrl shouldEqual "http://localhost:5555/assets/images/best-buy-logo.png"
    result.list.head.productCategory shouldEqual "special offer"
    result.list.head.price shouldEqual 699.99d
    result.list.head.numReviews shouldEqual 87
    result.list.head.rating shouldEqual 4.9f
  }

  test("search by keyword no results should return None") {
    val ws = MockWS {
      case (GET, "https://api.bestbuy.com/v1/products(search=sasdasdsd)") => actionBuilder {
        Ok(Json.parse(Source.fromFile(s"$MockPath/best_buy_search_no_results.json").getLines.mkString))
      }
    }
    val appConfigMock = mock[AppConfigService]
    when(appConfigMock.properties) thenReturn testConfigProperties

    val requestMonitorMock = mock[RequestMonitor]
    when(requestMonitorMock.isRequestPossible(BestBuy)) thenReturn true

    val repositoryDispatcher = getMockWorkerExecutionContext

    val service = new BestBuyRepositoryImpl(ws, appConfigMock, requestMonitorMock)(repositoryDispatcher)

    val p : Map[String,String] = Map(
      Name -> "sasdasdsd",
      Page -> "1"
    )

    val result = await(service.search(p))
    result shouldBe None
  }

  test("search by keyword when requestMonitor is Busy expect Future successful None") {
    val ws = MockWS {
      case (GET, "https://api.bestbuy.com/v1/products(search=prod123)") => actionBuilder { Ok }
    }
    val appConfigMock = mock[AppConfigService]
    when(appConfigMock.properties) thenReturn testConfigProperties

    val requestMonitorMock = mock[RequestMonitor]
    when(requestMonitorMock.isRequestPossible(BestBuy)) thenReturn false

    val repositoryDispatcher = getMockWorkerExecutionContext
    val service = new BestBuyRepositoryImpl(ws, appConfigMock, requestMonitorMock)(repositoryDispatcher)

    val p : Map[String,String] = Map(
      Name -> "xxxxxx123",
      Page -> "1"
    )

    val result = await(service.search(p))

    result shouldBe None
  }

  test("getProductDetail by Id should produce a valid JSON") {
    val ws = MockWS {
      case (GET, "https://api.bestbuy.com/v1/products(productId=5529006)") => actionBuilder {
        Ok(Json.parse(Source.fromFile(s"$MockPath/best_buy_get_by_id_prod_detail_response.json").getLines.mkString))
      }
    }

    val appConfigMock = mock[AppConfigService]
    when(appConfigMock.properties) thenReturn testConfigProperties
    when(appConfigMock.buildImgUrl(Some(any[String]))) thenReturn "http://localhost:5555/assets/images/best-buy-logo.png"
    when(appConfigMock.buildImgUrlExternal(Some(any[String]), any[Boolean])) thenReturn "https://img.bbystatic.com/BestBuy_US/images/products/5529/5529006_sa.jpg"

    val requestMonitorMock = mock[RequestMonitor]
    when(requestMonitorMock.isRequestPossible(BestBuy)) thenReturn true

    val repositoryDispatcher = getMockWorkerExecutionContext
    val service = new BestBuyRepositoryImpl(ws, appConfigMock, requestMonitorMock)(repositoryDispatcher)
    val result = await(service.getProductDetail("5529006", Id, Some(UnitedStates))).get

    validateProductDetail(result)
  }

  test("getProductDetail by Upc should produce a valid JSON") {
    val ws = MockWS {
      case (GET, "https://api.bestbuy.com/v1/products(upc=849803052423)") => actionBuilder {
        Ok(Json.parse(Source.fromFile(s"$MockPath/best_buy_get_by_upc_prod_detail_response.json").getLines.mkString))
      }
    }

    val appConfigMock = mock[AppConfigService]
    when(appConfigMock.properties) thenReturn testConfigProperties
    when(appConfigMock.buildImgUrl(Some(any[String]))) thenReturn "http://localhost:5555/assets/images/best-buy-logo.png"
    when(appConfigMock.buildImgUrlExternal(Some(any[String]), any[Boolean])) thenReturn "https://img.bbystatic.com/BestBuy_US/images/products/5529/5529006_sa.jpg"

    val requestMonitorMock = mock[RequestMonitor]
    when(requestMonitorMock.isRequestPossible(BestBuy)) thenReturn true

    val repositoryDispatcher = getMockWorkerExecutionContext
    val service = new BestBuyRepositoryImpl(ws, appConfigMock, requestMonitorMock)(repositoryDispatcher)
    val result = await(service.getProductDetail("849803052423", Upc, Some(UnitedStates))).get

    validateProductDetail(result)
  }

  test("getProductDetail using invalid Id should return None") {
    val ws = MockWS {
      case (GET, "https://api.bestbuy.com/v1/products(productId=123456)") => actionBuilder {
        Ok(Json.parse(Source.fromFile(s"$MockPath/best_buy_get_by_id_prod_detail_not_found.json").getLines.mkString))
      }
    }

    val appConfigMock = mock[AppConfigService]
    when(appConfigMock.properties) thenReturn testConfigProperties

    val requestMonitorMock = mock[RequestMonitor]
    when(requestMonitorMock.isRequestPossible(BestBuy)) thenReturn true

    val repositoryDispatcher = getMockWorkerExecutionContext
    val service = new BestBuyRepositoryImpl(ws, appConfigMock, requestMonitorMock)(repositoryDispatcher)
    val result = await(service.getProductDetail("123456", Id, Some(UnitedStates)))
    result shouldBe None
  }

  test("getProductDetail using invalid Upc should return None") {
    val ws = MockWS {
      case (GET, "https://api.bestbuy.com/v1/products(upc=123456)") => actionBuilder {
        Ok(Json.parse(Source.fromFile(s"$MockPath/best_buy_get_by_upc_prod_detail_not_found.json").getLines.mkString))
      }
    }

    val appConfigMock = mock[AppConfigService]
    when(appConfigMock.properties) thenReturn testConfigProperties

    val requestMonitorMock = mock[RequestMonitor]
    when(requestMonitorMock.isRequestPossible(BestBuy)) thenReturn true

    val repositoryDispatcher = getMockWorkerExecutionContext
    val service = new BestBuyRepositoryImpl(ws, appConfigMock, requestMonitorMock)(repositoryDispatcher)
    val result = await(service.getProductDetail("123456", Upc, Some(UnitedStates)))
    result shouldBe None
  }

  test("getProductDetail using invalid idType should return None") {
    val ws = MockWS {
      case (GET, "https://api.bestbuy.com/v1/products(productId=5529006)") => actionBuilder { Ok }
    }

    val appConfigMock = mock[AppConfigService]
    when(appConfigMock.properties) thenReturn testConfigProperties

    val requestMonitorMock = mock[RequestMonitor]
    when(requestMonitorMock.isRequestPossible(BestBuy)) thenReturn true

    val repositoryDispatcher = getMockWorkerExecutionContext
    val service = new BestBuyRepositoryImpl(ws, appConfigMock, requestMonitorMock)(repositoryDispatcher)

    val result = await(service.getProductDetail("5529006", "XYZ", Some(UnitedStates)))
    result shouldBe None
  }

  test("getProductDetail when requestMonitor is Busy expect Future successful None") {
    val ws = MockWS {
      case (GET, "https://api.bestbuy.com/v1/products(productId=5529006)") => actionBuilder { Ok }
    }

    val appConfigMock = mock[AppConfigService]
    when(appConfigMock.properties) thenReturn testConfigProperties

    val requestMonitorMock = mock[RequestMonitor]
    when(requestMonitorMock.isRequestPossible(BestBuy)) thenReturn false

    val repositoryDispatcher = getMockWorkerExecutionContext

    val service = new BestBuyRepositoryImpl(ws, appConfigMock, requestMonitorMock)(repositoryDispatcher)

    val result = await(service.getProductDetail("5529006", Id, Some(UnitedStates)))
    result shouldBe None
  }
}
package app.product.marketplace.amazon

import common.MockBaseUtil._
import common.config.AppConfigService
import mockws.MockWS
import org.junit.runner.RunWith
import org.mockito.Matchers._
import org.mockito.Mockito.when
import org.scalatest.junit.JUnitRunner
import org.scalatest.mockito.MockitoSugar
import org.scalatest.prop.PropertyChecks
import org.scalatest.{FunSuite, Matchers}
import play.api.mvc.Results._
import play.api.test.Helpers._
import product.marketplace.amazon.{AmazonRepositoryImpl, AmazonRequestHelper}
import product.marketplace.common.MarketplaceConstants._
import product.marketplace.common.RequestMonitor
import product.model.{ListRequest, OfferDetail}

import scala.collection.mutable.HashMap
import scala.xml.XML

@RunWith(classOf[JUnitRunner])
class AmazonRepositorySpec extends FunSuite with Matchers with PropertyChecks with MockitoSugar {

  val MockPath = s"$MockMarketplaceFilesPath/amazon"

  test("search should produce a valid JSON") {
    val ws = MockWS {
      case (GET, "https://webservices.amazon.com/signed") => actionBuilder {
        Ok(XML.loadFile(s"$MockPath/amazon_sample_search_response.xml"))
      }
    }

    val appConfigMock = mock[AppConfigService]
    when(appConfigMock.properties) thenReturn testConfigProperties
    when(appConfigMock.buildImgUrl(Some(any[String]))) thenReturn "http://localhost:5555/assets/images/amazon-logo.png"
    when(appConfigMock.buildImgUrlExternal(Some(any[String]), any[Boolean])) thenReturn "https://images-na.ssl-images-amazon.com/images/I/51AdOmJ2v%2BL.jpg"

    val requestMonitorMock = mock[RequestMonitor]
    when(requestMonitorMock.isRequestPossible(Amazon)) thenReturn true

    val amazonRequestHelperMock = mock[AmazonRequestHelper]
    when(amazonRequestHelperMock.sign(any[String], any[String], any[String], any[HashMap[String,String]])) thenReturn "https://webservices.amazon.com/signed"

    val repositoryDispatcher = getMockWorkerExecutionContext

    val service = new AmazonRepositoryImpl(ws, appConfigMock, requestMonitorMock, amazonRequestHelperMock)(repositoryDispatcher)

    // search by keywords
    val params = ListRequest.fromKeyword(ListRequest(), "skyrim")
    val result = await(service.search(params)).get

    result.summary.page shouldEqual 1
    result.summary.pageCount shouldEqual 6385
    result.summary.totalCount shouldEqual 63847
    result.list.head.id shouldEqual "B01GW8XJVU"
    result.list.head.upc shouldEqual Some("093155171251")
    result.list.head.name shouldEqual "The Elder Scrolls V: Skyrim - Special Edition - PlayStation 4"
    result.list.head.semanticName shouldEqual "https://www.amazon.com/Elder-Scrolls-Skyrim-Special-PlayStation-4/dp/B01GW8XJVU"
    result.list.head.partyName shouldEqual "amazon.com"
    result.list.head.mainImageFileUrl shouldEqual "https://images-na.ssl-images-amazon.com/images/I/51AdOmJ2v%2BL.jpg"
    result.list.head.partyImageFileUrl shouldEqual "http://localhost:5555/assets/images/amazon-logo.png"
    result.list.head.productCategory shouldEqual "Video Games"
    result.list.head.price shouldEqual 31.47d
    result.list.head.numReviews shouldEqual 0
    result.list.head.rating shouldEqual 0.0f
  }

  test("search with no params should return Random search term from config file") {
    val ws = MockWS {
      case (GET, "https://webservices.amazon.com/signed") => actionBuilder {
        Ok(XML.loadFile(s"$MockPath/amazon_sample_search_no_keyword.xml"))
      }
    }

    val appConfigMock = mock[AppConfigService]
    when(appConfigMock.properties) thenReturn testConfigProperties
    when(appConfigMock.buildImgUrl(Some(any[String]))) thenReturn "http://localhost:5555/assets/images/amazon-logo.png"
    when(appConfigMock.buildImgUrlExternal(Some(any[String]), any[Boolean])) thenReturn "https://images-na.ssl-images-amazon.com/images/I/51AdOmJ2v%2BL.jpg"

    val requestMonitorMock = mock[RequestMonitor]
    when(requestMonitorMock.isRequestPossible(Amazon)) thenReturn true

    val amazonRequestHelperMock = mock[AmazonRequestHelper]
    when(amazonRequestHelperMock.sign(any[String], any[String], any[String], any[HashMap[String,String]])) thenReturn "https://webservices.amazon.com/signed"

    val repositoryDispatcher = getMockWorkerExecutionContext

    val service = new AmazonRepositoryImpl(ws, appConfigMock, requestMonitorMock, amazonRequestHelperMock)(repositoryDispatcher)

    val result = await(service.search(ListRequest())).get

    result.summary.page shouldEqual 1
    result.summary.pageCount shouldEqual 372931
    result.summary.totalCount shouldEqual 3729310
    result.list.head.id shouldEqual "B01MRZFBBH"
    result.list.head.upc shouldEqual Some("886598045513")
    result.list.head.name shouldEqual "Huawei Honor 6X Dual Camera Unlocked Smartphone, 32GB Gray (US Warranty)"
    result.list.head.semanticName shouldEqual "https://www.amazon.com/Honor-6X-Unlocked-Smartphone-Gray/dp/B01MRZFBBH?psc=1"
    result.list.head.partyName shouldEqual "amazon.com"
    result.list.head.mainImageFileUrl shouldEqual "https://images-na.ssl-images-amazon.com/images/I/51AdOmJ2v%2BL.jpg"
    result.list.head.partyImageFileUrl shouldEqual "http://localhost:5555/assets/images/amazon-logo.png"
    result.list.head.productCategory shouldEqual "Wireless"
    result.list.head.price shouldEqual 199.00d
    result.list.head.numReviews shouldEqual 0
    result.list.head.rating shouldEqual 0.0f
  }

  test("search by keyword no results should return None") {
    val ws = MockWS {
      case (GET, "https://webservices.amazon.com/signed") => actionBuilder { Ok(XML.loadFile(s"$MockPath/amazon_search_no_results.xml")) }
    }

    val appConfigMock = mock[AppConfigService]
    when(appConfigMock.properties) thenReturn testConfigProperties

    val requestMonitorMock = mock[RequestMonitor]
    when(requestMonitorMock.isRequestPossible(Amazon)) thenReturn true

    val amazonRequestHelperMock = mock[AmazonRequestHelper]
    when(amazonRequestHelperMock.sign(any[String], any[String], any[String], any[HashMap[String,String]])) thenReturn "https://webservices.amazon.com/signed"

    val repositoryDispatcher = getMockWorkerExecutionContext
    val service = new AmazonRepositoryImpl(ws, appConfigMock, requestMonitorMock, amazonRequestHelperMock)(repositoryDispatcher)

    val params = ListRequest.fromKeyword(ListRequest(), "xxccxcxcxsdsxcxdsd")
    val result = await(service.search(params))
    result shouldBe None
  }

  test("search by keyword when requestMonitor is Busy expect Future successful None") {
    val ws = MockWS {
      case (GET, "https://webservices.amazon.com/signed") => actionBuilder { Ok }
    }
    val appConfigMock = mock[AppConfigService]
    when(appConfigMock.properties) thenReturn testConfigProperties

    val requestMonitorMock = mock[RequestMonitor]
    when(requestMonitorMock.isRequestPossible(Amazon)) thenReturn false

    val amazonRequestHelperMock = mock[AmazonRequestHelper]
    when(amazonRequestHelperMock.sign(any[String], any[String], any[String], any[HashMap[String,String]])) thenReturn "https://webservices.amazon.com/signed"

    val repositoryDispatcher = getMockWorkerExecutionContext
    val service = new AmazonRepositoryImpl(ws, appConfigMock, requestMonitorMock, amazonRequestHelperMock)(repositoryDispatcher)

    val params = ListRequest.fromKeyword(ListRequest(), "xxccxcxcxsdsxcxdsd")
    val result = await(service.search(params))
    result shouldBe None
  }

  private def validateProductDetail(result : OfferDetail, id: String, price: Double) = {
    result.offer.id shouldEqual id
    result.offer.upc shouldEqual Some("093155171251")
    result.offer.name shouldEqual "The Elder Scrolls V: Skyrim - Special Edition - PlayStation 4"
    result.offer.semanticName shouldEqual "https://www.amazon.com/Elder-Scrolls-Skyrim-Special-PlayStation-4/dp/B01GW8XJVU?psc=1&SubscriptionId=test12345678"
    result.offer.partyName shouldEqual "amazon.com"
    result.offer.mainImageFileUrl shouldEqual "https://images-na.ssl-images-amazon.com/images/I/51AdOmJ2v%2BL.jpg"
    result.offer.partyImageFileUrl shouldEqual "http://localhost:5555/assets/images/amazon-logo.png"
    result.offer.productCategory shouldEqual "Video Games"
    result.offer.price shouldEqual price
    result.offer.numReviews shouldEqual 0
    result.offer.rating shouldEqual 0.0f
  }

  test("getProductDetail by Id should produce a valid JSON") {
    val ws = MockWS {
      case (GET, "https://webservices.amazon.com/signed") => actionBuilder {
        Ok(XML.loadFile(s"$MockPath/amazon_get_product_detail_by_id.xml"))
      }
    }

    val appConfigMock = mock[AppConfigService]
    when(appConfigMock.properties) thenReturn testConfigProperties
    when(appConfigMock.buildImgUrl(Some(any[String]))) thenReturn "http://localhost:5555/assets/images/amazon-logo.png"
    when(appConfigMock.buildImgUrlExternal(Some(any[String]), any[Boolean])) thenReturn "https://images-na.ssl-images-amazon.com/images/I/51AdOmJ2v%2BL.jpg"

    val requestMonitorMock = mock[RequestMonitor]
    when(requestMonitorMock.isRequestPossible(Amazon)) thenReturn true

    val amazonRequestHelperMock = mock[AmazonRequestHelper]
    when(amazonRequestHelperMock.sign(any[String], any[String], any[String], any[HashMap[String,String]])) thenReturn "https://webservices.amazon.com/signed"

    val repositoryDispatcher = getMockWorkerExecutionContext

    val service = new AmazonRepositoryImpl(ws, appConfigMock, requestMonitorMock, amazonRequestHelperMock)(repositoryDispatcher)
    val result = await(service.getProductDetail("B01GW8XJVU", Id, Amazon, Some(UnitedStates))).get
    validateProductDetail(result, "B01GW8XJVU", 25.0)
  }

  test("getProductDetail using invalid Id should return None") {
    val ws = MockWS {
      case (GET, "https://webservices.amazon.com/signed") => actionBuilder {
        Ok(XML.loadFile(s"$MockPath/amazon_get_product_detail_by_id_not_found.xml"))
      }
    }

    val appConfigMock = mock[AppConfigService]
    when(appConfigMock.properties) thenReturn testConfigProperties

    val requestMonitorMock = mock[RequestMonitor]
    when(requestMonitorMock.isRequestPossible(Amazon)) thenReturn true

    val amazonRequestHelperMock = mock[AmazonRequestHelper]
    when(amazonRequestHelperMock.sign(any[String], any[String], any[String], any[HashMap[String,String]])) thenReturn "https://webservices.amazon.com/signed"

    val repositoryDispatcher = getMockWorkerExecutionContext

    val service = new AmazonRepositoryImpl(ws, appConfigMock, requestMonitorMock, amazonRequestHelperMock)(repositoryDispatcher)
    val result = await(service.getProductDetail("XYZ12345678", Id, Amazon, Some(UnitedStates)))

    result shouldBe None
  }

  test("getProductDetail by Upc should produce a valid JSON") {
    val ws = MockWS {
      case (GET, "https://webservices.amazon.com/signed") => actionBuilder {
        Ok(XML.loadFile(s"$MockPath/amazon_get_product_detail_by_upc.xml"))
      }
    }

    val appConfigMock = mock[AppConfigService]
    when(appConfigMock.properties) thenReturn testConfigProperties
    when(appConfigMock.buildImgUrl(Some(any[String]))) thenReturn "http://localhost:5555/assets/images/amazon-logo.png"
    when(appConfigMock.buildImgUrlExternal(Some(any[String]), any[Boolean])) thenReturn "https://images-na.ssl-images-amazon.com/images/I/51AdOmJ2v%2BL.jpg"

    val requestMonitorMock = mock[RequestMonitor]
    when(requestMonitorMock.isRequestPossible(Amazon)) thenReturn true

    val amazonRequestHelperMock = mock[AmazonRequestHelper]
    when(amazonRequestHelperMock.sign(any[String], any[String], any[String], any[HashMap[String,String]])) thenReturn "https://webservices.amazon.com/signed"

    val repositoryDispatcher = getMockWorkerExecutionContext

    val service = new AmazonRepositoryImpl(ws, appConfigMock, requestMonitorMock, amazonRequestHelperMock)(repositoryDispatcher)
    val result = await(service.getProductDetail("093155171251", Upc, Amazon, Some(UnitedStates))).get

    validateProductDetail(result, "B01GW8XJVU", 28.99)
  }

  test("getProductDetail using invalid Upc should return None") {
    val ws = MockWS {
      case (GET, "https://webservices.amazon.com/signed") => actionBuilder {
        Ok(XML.loadFile(s"$MockPath/amazon_get_product_detail_by_upc_not_found.xml"))
      }
    }

    val appConfigMock = mock[AppConfigService]
    when(appConfigMock.properties) thenReturn testConfigProperties

    val requestMonitorMock = mock[RequestMonitor]
    when(requestMonitorMock.isRequestPossible(Amazon)) thenReturn true

    val amazonRequestHelperMock = mock[AmazonRequestHelper]
    when(amazonRequestHelperMock.sign(any[String], any[String], any[String], any[HashMap[String,String]])) thenReturn "https://webservices.amazon.com/signed"

    val repositoryDispatcher = getMockWorkerExecutionContext

    val service = new AmazonRepositoryImpl(ws, appConfigMock, requestMonitorMock, amazonRequestHelperMock)(repositoryDispatcher)
    val result = await(service.getProductDetail("23456789251", Upc, Amazon, Some(UnitedStates)))

    result shouldBe None
  }

  test("getProductDetail using invalid idType should return None") {
    val ws = MockWS {
      case (GET, "https://webservices.amazon.com/signed") => actionBuilder { Ok }
    }

    val appConfigMock = mock[AppConfigService]
    when(appConfigMock.properties) thenReturn testConfigProperties

    val requestMonitorMock = mock[RequestMonitor]
    when(requestMonitorMock.isRequestPossible(Amazon)) thenReturn true

    val amazonRequestHelperMock = mock[AmazonRequestHelper]
    when(amazonRequestHelperMock.sign(any[String], any[String], any[String], any[HashMap[String,String]])) thenReturn "https://webservices.amazon.com/signed"

    val repositoryDispatcher = getMockWorkerExecutionContext

    val service = new AmazonRepositoryImpl(ws, appConfigMock, requestMonitorMock, amazonRequestHelperMock)(repositoryDispatcher)
    val result = await(service.getProductDetail("prod123456", "XYZ", Amazon, Some(UnitedStates)))

    result shouldBe None
  }

  test("getProductDetail when requestMonitor is Busy expect Future successful None") {
    val ws = MockWS {
      case (GET, "https://webservices.amazon.com/signed") => actionBuilder { Ok }
    }

    val appConfigMock = mock[AppConfigService]
    when(appConfigMock.properties) thenReturn testConfigProperties

    val requestMonitorMock = mock[RequestMonitor]
    when(requestMonitorMock.isRequestPossible(Amazon)) thenReturn false

    val amazonRequestHelperMock = mock[AmazonRequestHelper]
    when(amazonRequestHelperMock.sign(any[String], any[String], any[String], any[HashMap[String,String]])) thenReturn "https://webservices.amazon.com/signed"

    val repositoryDispatcher = getMockWorkerExecutionContext

    val service = new AmazonRepositoryImpl(ws, appConfigMock, requestMonitorMock, amazonRequestHelperMock)(repositoryDispatcher)
    val result = await(service.getProductDetail("prod123456", Id, Amazon, Some(UnitedStates)))

    result shouldBe None
  }

}

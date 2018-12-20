package app.product

import common.BaseDomainTest
import common.MockBaseUtil._
import common.config.AppConfigService
import mockws.MockWS
import org.junit.runner.RunWith
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.junit.JUnitRunner
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.libs.ws.WSClient
import play.api.mvc.Results.Ok
import play.api.test.CSRFTokenHelper._
import play.api.test.Helpers._
import play.api.test.{FakeRequest, WithApplication, _}
import product.marketplace.amazon.AmazonRequestHelper

import scala.collection.mutable.HashMap
import scala.io.Source
import scala.xml.XML

@RunWith(classOf[JUnitRunner])
class ProductControllerPostSearchNotFoundSpec extends BaseDomainTest {

  val ws = MockWS {
    case (GET, "http://api.walmartlabs.com/v1/search") => Action {
      Ok(Json.parse(Source.fromFile(s"$MockMarketplaceFilesPath/walmart/walmart_sample_search_no_results.json").getLines.mkString))
    }

    case (GET, "http://svcs.ebay.com/services/search/FindingService/v1") => Action {
      Ok(Json.parse(Source.fromFile(s"$MockMarketplaceFilesPath/ebay/ebay_sample_search_no_results.json").getLines.mkString))
    }

    case (GET, "https://webservices.amazon.com/signed") => Action {
      Ok(XML.loadFile(s"$MockMarketplaceFilesPath/amazon/amazon_sample_search_no_results.xml"))
    }

    case (GET, "https://api.bestbuy.com/v1/products(search=abc123)") => Action {
      Ok(Json.parse(Source.fromFile(s"$MockMarketplaceFilesPath/bestbuy/best_buy_search_no_results.json").getLines.mkString))
    }
  }

  val amazonRequestHelperMock = mock[AmazonRequestHelper]
  when(amazonRequestHelperMock.sign(any[String], any[String], any[String], any[HashMap[String,String]])) thenReturn "https://webservices.amazon.com/signed"

  val appConfigMock = mock[AppConfigService]
  when(appConfigMock.properties) thenReturn testConfigProperties
  when(appConfigMock.buildImgUrl(Some(any[String]))) thenReturn "https://localhost:5555/assets/images/product-img.png"
  when(appConfigMock.buildImgUrlExternal(Some(any[String]), any[Boolean])) thenReturn "https://localhost:5555/assets/images/product-img-01.jpg"

  val appMock = new GuiceApplicationBuilder()
    .overrides(bind[AppConfigService].toInstance(appConfigMock))
    .overrides(bind[AmazonRequestHelper].toInstance(amazonRequestHelperMock))
    .overrides(bind[WSClient].toInstance(ws))
    .build

  "search by keyword no results should return Total results zero" in new WithApplication(appMock) with WsTestClient {
    val request = FakeRequest(POST, "/api/v1/products").withHeaders(HOST -> "localhost:9000").withCSRFToken.withBody(
      Json.parse(Source.fromFile(s"$MockFilesPath/sample_search_request_not_found.json").getLines.mkString))
    val response = route(app, request).get
    val json = contentAsJson(response)
    val summary = json \ "summary"

    status(response) mustBe OK
    (summary \ "page").as[Int] mustBe 0
    (summary \ "pageCount").as[Int] mustBe 0
    (summary \ "totalCount").as[Int] mustBe 0
  }

}
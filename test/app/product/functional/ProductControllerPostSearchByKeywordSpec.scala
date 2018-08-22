package app.product.functional

import common.MockBaseUtil._
import common.config.AppConfigService
import mockws.MockWS
import org.junit.runner.RunWith
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.junit.JUnitRunner
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsLookupResult, JsValue, Json}
import play.api.libs.ws.WSClient
import play.api.mvc.Results.Ok
import play.api.mvc.{Request, Result}
import play.api.test.CSRFTokenHelper._
import play.api.test.Helpers._
import play.api.test.{FakeRequest, WithApplication, _}
import product.marketplace.amazon.AmazonRequestHelper

import scala.collection.mutable
import scala.concurrent.Future
import scala.io.Source
import scala.xml.XML

@RunWith(classOf[JUnitRunner])
class ProductControllerPostSearchByKeywordSpec extends PlaySpec with MockitoSugar {

  val ws = MockWS {
    case (GET, "http://api.walmartlabs.com/v1/search") => actionBuilder {
      Ok(Json.parse(Source.fromFile(s"$MockMarketplaceFilesPath/walmart/walmart_sample_search_response.json").getLines.mkString))
    }

    case (GET, "http://svcs.ebay.com/services/search/FindingService/v1") => actionBuilder {
      Ok(Json.parse(Source.fromFile(s"$MockMarketplaceFilesPath/ebay/ebay_sample_search_response.json").getLines.mkString))
    }

    case (GET, "https://webservices.amazon.com/signed") => actionBuilder {
      Ok(XML.loadFile(s"$MockMarketplaceFilesPath/amazon/amazon_sample_search_response.xml"))
    }

    case (GET, "https://api.bestbuy.com/v1/products(search=skyrim)") => actionBuilder {
      Ok(Json.parse(Source.fromFile(s"$MockMarketplaceFilesPath/bestbuy/best_buy_sample_search_response.json").getLines.mkString))
    }
  }

  val jsonRequest: JsValue = Json.parse(Source.fromFile(s"$MockFilesPath/sample_search_request.json").getLines.mkString)

  val amazonRequestHelperMock: AmazonRequestHelper = mock[AmazonRequestHelper]
  when(amazonRequestHelperMock.sign(any[String], any[String], any[String], any[mutable.HashMap[String,String]])) thenReturn "https://webservices.amazon.com/signed"

  val appConfigMock: AppConfigService = mock[AppConfigService]
  when(appConfigMock.properties) thenReturn testConfigProperties
  when(appConfigMock.buildImgUrl(Some(any[String]))) thenReturn "https://localhost:5555/assets/images/product-img.png"
  when(appConfigMock.buildImgUrlExternal(Some(any[String]), any[Boolean])) thenReturn "https://localhost:5555/assets/images/product-img-01.jpg"

  val appMock: Application = new GuiceApplicationBuilder()
    .overrides(bind[AppConfigService].toInstance(appConfigMock))
    .overrides(bind[AmazonRequestHelper].toInstance(amazonRequestHelperMock))
    .overrides(bind[WSClient].toInstance(ws))
    .build

  "search by keyword" in new WithApplication(appMock) with WsTestClient {
    val request: Request[JsValue] = FakeRequest(POST, "/api/v1/products").withHeaders(HOST -> "localhost:9000").withCSRFToken.withBody(jsonRequest)
    val response: Future[Result] = route(app, request).get
    val json: JsValue = contentAsJson(response)

    val elem0: JsValue = (json \ "list")(0)
    val elem1: JsValue = (json \ "list")(1)
    val summary: JsLookupResult = json \ "summary"

    status(response) mustBe OK
    (summary \ "page").as[Int] mustBe 1
    (summary \ "pageCount").as[Int] mustBe 7050
    (summary \ "totalCount").as[Int] mustBe 70494
    (elem0 \ "id").as[String] != "" mustBe true
    (elem0 \ "name").as[String] != "" mustBe true
    (elem0 \ "partyName").as[String] != "" mustBe true
    (elem1 \ "id").as[String] != "" mustBe true
    (elem1 \ "name").as[String] != "" mustBe true
    (elem1 \ "partyName").as[String] != "" mustBe true
  }

}
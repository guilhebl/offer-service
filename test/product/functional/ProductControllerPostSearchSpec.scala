package product.functional

import app.product.marketplace.amazon.AmazonRequestHelper
import common.config.AppConfigService
import mockws.MockWS
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsValue, Json}
import play.api.libs.ws.WSClient
import play.api.mvc.Results.Ok
import play.api.test.CSRFTokenHelper._
import play.api.test.Helpers._
import play.api.test.{FakeRequest, WithApplication, _}
import product.util.MockBaseUtil._

import scala.collection.mutable.HashMap
import scala.io.Source
import scala.xml.XML

/**
  * Functional Test for:
  *
  * Product Controller Search - No Keywords
  *
  * expects to query external Trending APIs for some external clients
  * and use default search values for others that don't have a trending API available.
  *
  * @author gui
  */
class ProductControllerPostSearchSpec extends PlaySpec with MockitoSugar {

  val ws = MockWS {
    case (GET, "http://api.walmartlabs.com/v1/trends") => actionBuilder {
      Ok(Json.parse(Source.fromFile(s"$MockMarketplaceFilesPath/walmart/walmart_sample_trend_response.json").getLines.mkString))
    }

    case (GET, "http://svcs.ebay.com/services/search/FindingService/v1") => actionBuilder {
      Ok(Json.parse(Source.fromFile(s"$MockMarketplaceFilesPath/ebay/ebay_sample_search_no_keyword.json").getLines.mkString))
    }

    case (GET, "https://webservices.amazon.com/signed") => actionBuilder {
      Ok(XML.loadFile(s"$MockMarketplaceFilesPath/amazon/amazon_sample_search_no_keyword.xml"))
    }

    case (GET, "https://api.bestbuy.com/beta/products/trendingViewed") => actionBuilder {
      Ok(Json.parse(Source.fromFile(s"$MockMarketplaceFilesPath/bestbuy/best_buy_sample_trending_response.json").getLines.mkString))
    }
  }

  val amazonRequestHelperMock = mock[AmazonRequestHelper]
  when(amazonRequestHelperMock.sign(any[String], any[String], any[String], any[HashMap[String,String]])) thenReturn "https://webservices.amazon.com/signed"

  val appConfigMock = mock[AppConfigService]
  when(appConfigMock.properties) thenReturn testConfigProperties

  val appMock = new GuiceApplicationBuilder()
    .overrides(bind[AppConfigService].toInstance(appConfigMock))
    .overrides(bind[AmazonRequestHelper].toInstance(amazonRequestHelperMock))
    .overrides(bind[WSClient].toInstance(ws))
    .build

  "search" in new WithApplication(appMock) with WsTestClient {
    callWebservice(app, "search_request_empty.json", (json) => {
      val summary = (json \ "summary")
      (summary \ "page").as[Int] mustBe 1
      (summary \ "pageCount").as[Int] mustBe 3590324
      (summary \ "totalCount").as[Int] mustBe 35903240
    })

    callWebservice(app, "search_request_no_keyword_sort_by_id_asc.json", (json) => {
      val elem = (json \ "list")(0)
      val elem1 = (json \ "list")(1)
      (elem \ "id").as[String] mustBe "13812591"
      (elem1 \ "id").as[String] mustBe "17128612"
    })

    callWebservice(app, "search_request_no_keyword_sort_by_id_desc.json", (json) => {
      val elem = (json \ "list")(0)
      val elem1 = (json \ "list")(1)
      (elem \ "id").as[String] mustBe "B075L3YXZF"
      (elem1 \ "id").as[String] mustBe "B0743W4Y75"
    })

    callWebservice(app, "search_request_no_keyword_sort_by_name_asc.json", (json) => {
      val elem = (json \ "list")(0)
      val elem1 = (json \ "list")(1)
      (elem \ "name").as[String] mustBe """26" Hyper Summit Men's Mountain Bike"""
      (elem1 \ "name").as[String] startsWith "Acer Aspire ES 15 Series ES1-572-33BP 15.6" mustBe true
    })

    callWebservice(app, "search_request_no_keyword_sort_by_name_desc.json", (json) => {
      val elem = (json \ "list")(0)
      val elem1 = (json \ "list")(1)
      (elem \ "name").as[String] mustBe "Yu-Gi-Oh! 2016 Legendary Decks II Collection Box"
      (elem1 \ "name").as[String] startsWith "Wingtech Bluetooth Smartwatch 1.54 Inch Touch Screen Smart Wrist" mustBe true
    })

    callWebservice(app, "search_request_no_keyword_sort_by_price_asc.json", (json) => {
      val elem = (json \ "list")(0)
      val elem1 = (json \ "list")(1)
      (elem \ "price").as[Float] mustBe 8.0f
      (elem1 \ "price").as[Float] mustBe 24.99f
    })

    callWebservice(app, "search_request_no_keyword_sort_by_price_desc.json", (json) => {
      val elem = (json \ "list")(0)
      val elem1 = (json \ "list")(1)
      (elem \ "price").as[Float] mustBe 1549.99f
      (elem1 \ "price").as[Float] mustBe 999.99f
    })

    callWebservice(app, "search_request_no_keyword_sort_by_rating_asc.json", (json) => {
      val elem = (json \ "list")(0)
      val elem1 = (json \ "list")(1)
      (elem \ "rating").as[Float] mustBe 0.0f
      (elem1 \ "rating").as[Float] mustBe 0.0f
    })

    callWebservice(app, "search_request_no_keyword_sort_by_rating_desc.json", (json) => {
      val elem = (json \ "list")(0)
      val elem1 = (json \ "list")(1)
      (elem \ "rating").as[Float] mustBe 5.0f
      (elem1 \ "rating").as[Float] mustBe 5.0f
    })

    callWebservice(app, "search_request_no_keyword_sort_by_num_reviews_asc.json", (json) => {
      val elem = (json \ "list")(0)
      val elem1 = (json \ "list")(1)
      (elem \ "numReviews").as[Int] mustBe 0
      (elem1 \ "numReviews").as[Int] mustBe 0
    })

    callWebservice(app, "search_request_no_keyword_sort_by_num_reviews_desc.json", (json) => {
      val elem = (json \ "list")(0)
      val elem1 = (json \ "list")(1)
      (elem \ "numReviews").as[Int] mustBe 7538
      (elem1 \ "numReviews").as[Int] mustBe 6487
    })

  }

  def callWebservice(app: Application, mockRequestJsonFile: String, validator: JsValue => Unit) : Unit = {
    val req = FakeRequest(POST, "/products").withHeaders(HOST -> "localhost:9000").withCSRFToken.withBody(
      Json.parse(Source.fromFile(s"$MockFilesPath/$mockRequestJsonFile").getLines.mkString))
    val response = route(app, req).get
    validator(contentAsJson(response))
  }

}
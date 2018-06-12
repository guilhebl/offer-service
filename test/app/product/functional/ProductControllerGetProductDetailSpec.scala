package app.product.functional

import common.MockBaseUtil._
import common.config.AppConfigService
import common.db.MongoRepository
import mockws.MockWS
import org.junit.runner.RunWith
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.junit.JUnitRunner
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.libs.ws.WSClient
import play.api.mvc.Results.Ok
import play.api.test.Helpers._
import play.api.test.{FakeRequest, WithApplication, _}
import product.marketplace.amazon.AmazonRequestHelper

import scala.collection.mutable.HashMap
import scala.io.Source
import scala.xml.XML

@RunWith(classOf[JUnitRunner])
class ProductControllerGetProductDetailSpec extends PlaySpec with MockitoSugar {

  val ws = MockWS {
    case (GET, "http://api.walmartlabs.com/v1/items") => actionBuilder {
      Ok(Json.parse(Source.fromFile(s"$MockMarketplaceFilesPath/walmart/walmart_sample_upc_prod_detail_response.json").getLines.mkString))
    }

    case (GET, "http://svcs.ebay.com/services/search/FindingService/v1") => actionBuilder {
      Ok(Json.parse(Source.fromFile(s"$MockMarketplaceFilesPath/ebay/ebay_find_by_upc.json").getLines.mkString))
    }

    case (GET, "https://webservices.amazon.com/signed") => actionBuilder {
      Ok(XML.loadFile(s"$MockMarketplaceFilesPath/amazon/amazon_get_product_detail_by_upc.xml"))
    }

    case (GET, "https://api.bestbuy.com/v1/products(productId=5529006)") => actionBuilder {
      Ok(Json.parse(Source.fromFile(s"$MockMarketplaceFilesPath/bestbuy/best_buy_get_by_id_prod_detail_response.json").getLines.mkString))
    }
  }

  val amazonRequestHelperMock = mock[AmazonRequestHelper]
  when(amazonRequestHelperMock.sign(any[String], any[String], any[String], any[HashMap[String,String]])) thenReturn "https://webservices.amazon.com/signed"

  val appConfigMock = mock[AppConfigService]
  when(appConfigMock.properties) thenReturn testConfigProperties
  when(appConfigMock.buildImgUrl(Some(any[String]))) thenReturn "http://localhost:5555/assets/images/logo.png"
  when(appConfigMock.buildImgUrlExternal(Some(any[String]), any[Boolean])) thenReturn "https://localhost/images/I/51AdOmJ2vBL.jpg"

  val mongoRepositoryMock = mock[MongoRepository]

  val appMock = new GuiceApplicationBuilder()
    .overrides(bind[AppConfigService].toInstance(appConfigMock))
    .overrides(bind[AmazonRequestHelper].toInstance(amazonRequestHelperMock))
    .overrides(bind[MongoRepository].toInstance(mongoRepositoryMock))
    .overrides(bind[WSClient].toInstance(ws))
    .build

  "get product by Id" in new WithApplication(appMock) with WsTestClient {

    val request = FakeRequest(GET, "/products/5529006?idType=id&source=bestbuy.com").withHeaders(HOST -> "localhost:9000")

    val response = route(app, request).get
    val json = contentAsJson(response)
    val elem0 = json \ "offer"
    val attr0 = (json \ "attributes")(0)
    val pdi0 = (json \ "productDetailItems")(0)

    status(response) mustBe OK
    (elem0 \ "id").as[String] mustBe "5529006"
    (elem0 \ "upc").as[String] mustBe "849803052423"
    (elem0 \ "name").as[String] mustBe "Funko - Elder Scrolls V: Skyrim Dovahkiin Pop! Vinyl Figure"
    (elem0 \ "partyName").as[String] mustBe "bestbuy.com"
    (elem0 \ "semanticName").as[String] mustBe "https://api.bestbuy.com/click/-/5529006/pdp"
    (elem0 \ "productCategory").as[String] mustBe "Best Buy-Drones, Toys & Collectibles-Licensed Collectibles-Video Game Collectibles"
    (elem0 \ "mainImageFileUrl").as[String] mustBe "https://localhost/images/I/51AdOmJ2vBL.jpg"
    (elem0 \ "partyImageFileUrl").as[String] mustBe "http://localhost:5555/assets/images/logo.png"
    (elem0 \ "price").as[Double] mustBe 9.99
    (elem0 \ "rating").as[Float] mustBe 4.8f
    (elem0 \ "numReviews").as[Int] mustBe 28
    (elem0 \ "numReviews").as[Int] mustBe 28
    (attr0 \ "name").as[String] mustBe "manufacturer"
    (attr0 \ "value").as[String] mustBe "Funko"
    (pdi0 \ "partyName").as[String] mustBe "amazon.com"
    (pdi0 \ "semanticName").as[String] mustBe "https://www.amazon.com/Elder-Scrolls-Skyrim-Special-PlayStation-4/dp/B01GW8XJVU?psc=1&SubscriptionId=test12345678"
    (pdi0 \ "partyImageFileUrl").as[String] mustBe "http://localhost:5555/assets/images/logo.png"
    (pdi0 \ "price").as[Double] mustBe 28.99
    (pdi0 \ "rating").as[Float] mustBe 0.0
    (pdi0 \ "numReviews").as[Int] mustBe 0
  }

}
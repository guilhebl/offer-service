package app.product.functional

import app.product.marketplace.amazon.AmazonRequestHelper
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
import play.api.libs.json.{JsNull, Json}
import play.api.libs.ws.WSClient
import play.api.mvc.Results.Ok
import play.api.test.Helpers._
import play.api.test.{FakeRequest, WithApplication, _}

import scala.collection.mutable.HashMap
import scala.io.Source
import scala.xml.XML

@RunWith(classOf[JUnitRunner])
class ProductControllerGetProductDetailNotFoundSpec extends PlaySpec with MockitoSugar {

  val ws = MockWS {
    case (GET, "http://api.walmartlabs.com/v1/items/12345678") => actionBuilder {
      Ok(Json.parse(Source.fromFile(s"$MockMarketplaceFilesPath/walmart/walmart_get_by_id_invalid.json").getLines.mkString))
    }

    case (GET, "http://svcs.ebay.com/services/search/FindingService/v1") => actionBuilder {
      Ok(Json.parse(Source.fromFile(s"$MockMarketplaceFilesPath/ebay/ebay_find_by_id_no_result.json").getLines.mkString))
    }

    case (GET, "https://webservices.amazon.com/signed") => actionBuilder {
      Ok(XML.loadFile(s"$MockMarketplaceFilesPath/amazon/amazon_get_product_detail_by_id_not_found.xml"))
    }

    case (GET, "https://api.bestbuy.com/v1/products(productId=12345678)") => actionBuilder {
      Ok(Json.parse(Source.fromFile(s"$MockMarketplaceFilesPath/bestbuy/best_buy_get_by_id_prod_detail_not_found.json").getLines.mkString))
    }
  }

  val amazonRequestHelperMock = mock[AmazonRequestHelper]
  when(amazonRequestHelperMock.sign(any[String], any[String], any[String], any[HashMap[String,String]])) thenReturn "https://webservices.amazon.com/signed"

  val appConfigMock = mock[AppConfigService]
  when(appConfigMock.properties) thenReturn testConfigProperties
  when(appConfigMock.buildImgUrl(Some(any[String]))) thenReturn "http://localhost:5555/assets/images/logo.png"
  when(appConfigMock.buildImgUrlExternal(Some(any[String]), any[Boolean])) thenReturn "https://localhost/images/I/51AdOmJ2vBL.jpg"

  val appMock = new GuiceApplicationBuilder()
    .overrides(bind[AppConfigService].toInstance(appConfigMock))
    .overrides(bind[AmazonRequestHelper].toInstance(amazonRequestHelperMock))
    .overrides(bind[WSClient].toInstance(ws))
    .build

  private def expectResultEmpty(application : Application, url : String) = {
    val request2 = FakeRequest(GET, url).withHeaders(HOST -> "localhost:9000")
    val response2 = route(application, request2).get
    val json2 = contentAsJson(response2)
    json2 mustBe JsNull
  }

  "get app.product by Id NotFound should return None for all marketplace providers" in new WithApplication(appMock) with WsTestClient {
    expectResultEmpty(app, "/products/12345678?idType=id&source=walmart.com")
    expectResultEmpty(app, "/products/12345678?idType=id&source=ebay.com")
    expectResultEmpty(app, "/products/12345678?idType=id&source=amazon.com")
    expectResultEmpty(app, "/products/12345678?idType=id&source=bestbuy.com")
  }

}
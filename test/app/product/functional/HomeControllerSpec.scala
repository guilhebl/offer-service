package app.product.functional

import common.BaseDomainTest
import common.MockBaseUtil._
import common.config.AppConfigService
import mockws.MockWS
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import org.scalatestplus.play.{HtmlUnitFactory, OneBrowserPerSuite}
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.libs.ws.WSClient
import play.api.mvc.Results.Ok
import play.api.test.Helpers.GET
import product.marketplace.amazon.AmazonRequestHelper

import scala.collection.mutable.HashMap
import scala.io.Source
import scala.xml.XML

class HomeControllerSpec extends BaseDomainTest with GuiceOneServerPerSuite with OneBrowserPerSuite with HtmlUnitFactory {

  val ws = MockWS {
    case (GET, "http://api.walmartlabs.com/v1/search") => Action {
      Ok(Json.parse(Source.fromFile(s"$MockMarketplaceFilesPath/walmart/walmart_sample_search_response.json").getLines.mkString))
    }

    case (GET, "http://svcs.ebay.com/services/search/FindingService/v1") => Action {
      Ok(Json.parse(Source.fromFile(s"$MockMarketplaceFilesPath/ebay/ebay_sample_search_response.json").getLines.mkString))
    }

    case (GET, "https://webservices.amazon.com/signed") => Action {
      Ok(XML.loadFile(s"$MockMarketplaceFilesPath/amazon/amazon_sample_search_response.xml"))
    }

    case (GET, "https://api.bestbuy.com//v1/products(search=skyrim)") => Action {
      Ok(Json.parse(Source.fromFile(s"$MockMarketplaceFilesPath/bestbuy/best_buy_sample_search_response.json").getLines.mkString))
    }
  }

  val amazonRequestHelperMock = mock[AmazonRequestHelper]
  when(amazonRequestHelperMock.sign(any[String], any[String], any[String], any[HashMap[String,String]])) thenReturn "https://webservices.amazon.com/signed"

  val appConfigMock = mock[AppConfigService]
  when(appConfigMock.properties) thenReturn testConfigProperties
  when(appConfigMock.buildImgUrl(Some(any[String]))) thenReturn "https://localhost:5555/assets/images/product-img.png"
  when(appConfigMock.buildImgUrlExternal(Some(any[String]), any[Boolean])) thenReturn "https://localhost:5555/assets/images/product-img-01.jpg"

  override def fakeApplication() = new GuiceApplicationBuilder()
    .overrides(bind[AppConfigService].toInstance(appConfigMock))
    .overrides(bind[AmazonRequestHelper].toInstance(amazonRequestHelperMock))
    .overrides(bind[WSClient].toInstance(ws))
    .build

  "The OneBrowserPerTest trait" must {
    "provide a web driver" in {
      go to s"http://localhost:$port/"
      pageTitle mustBe "Search Prod REST API"
    }
  }

}

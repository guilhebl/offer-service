package app.product

import org.scalatestplus.play._
import org.scalatestplus.play.guice._
import play.api.test.CSRFTokenHelper._
import play.api.test.Helpers._
import play.api.test._

class HomeControllerViewSpec extends PlaySpec with GuiceOneAppPerTest {

  "HomeController" should {

    "render the index page" in {
      val request = FakeRequest(GET, "/").withHeaders(HOST -> "localhost:9000").withCSRFToken
      val home = route(app, request).get

      contentAsString(home) must include ("searchprod REST API")
    }

  }

}
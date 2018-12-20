package app.product

import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import product.{ProductController, ProductRouter}

class ProductRouterSpec extends PlaySpec with MockitoSugar {
  
  "ProductRouter#link" should {
    "return link" in {
      val mockCtrl = mock[ProductController]      
      val obj = new ProductRouter(mockCtrl)

      val v = obj.link("1")
      v mustEqual "/api/v1/products/1"
    }
  }
  
  "ProductRouter#routes" should {
    "return routes" in {
      
      val mockCtrl = mock[ProductController]
      val obj = new ProductRouter(mockCtrl)
      val routes = obj.routes      
      
      (routes != null) mustBe true
    }
  }
  
}
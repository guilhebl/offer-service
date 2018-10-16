package product.marketplace.bestbuy.model

import play.api.libs.json._

case class ProductItem(
  productId : Option[Long],
  upc : Option[String],
  sku : Option[Long],
  name : String,
  salePrice : Double,
  releaseDate : Option[String],
  url : Option[String],
  image : Option[String],
  thumbnailImage : String,
  manufacturer : Option[String],
  department : Option[String],
  customerReviewAverage : Option[Float],
  customerReviewCount : Option[Int],
  categoryPath : Vector[CategoryPath]
)

object ProductItem {
  implicit val formatter = Json.format[ProductItem]        
}
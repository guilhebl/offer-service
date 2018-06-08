package product.marketplace.bestbuy.model

import play.api.libs.json._

case class CategoryPath(
    name : String
)

object CategoryPath {
  implicit val formatter = Json.format[CategoryPath]        
}
package product.marketplace.ebay.model

import play.api.libs.json._

case class PrimaryCategory (
    categoryId : Vector[String],    
    categoryName : Vector[String]
)

object PrimaryCategory {
  implicit val formatter = Json.format[PrimaryCategory]        
}

package app.product.marketplace.ebay.model

import play.api.libs.json._

case class PrimaryCategory (
    categoryId : Iterable[String],    
    categoryName : Iterable[String]
)

object PrimaryCategory {
  implicit val formatter = Json.format[PrimaryCategory]        
}

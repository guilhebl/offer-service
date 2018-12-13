package product.marketplace.bestbuy.model

import play.api.libs.json.Json

case class ProductImages (
    standard : Option[String]
)

object ProductImages {
  implicit val formatter = Json.format[ProductImages]        
}
package product.marketplace.bestbuy.model

import play.api.libs.json.Json

case class ProductImages (
    standard : String
)

object ProductImages {
  implicit val formatter = Json.format[ProductImages]        
}
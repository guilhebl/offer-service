package app.product.marketplace.bestbuy.model

import play.api.libs.json.Json

case class ProductLinks(
    product : String,
    web : String,
    addToCart : String
)

object ProductLinks {
  implicit val formatter = Json.format[ProductLinks]        
}
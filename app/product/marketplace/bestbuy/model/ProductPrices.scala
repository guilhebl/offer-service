package product.marketplace.bestbuy.model

import play.api.libs.json.Json

case class ProductPrices(
    current : Double,
    regular : Double
)

object ProductPrices {
  implicit val formatter = Json.format[ProductPrices]        
}

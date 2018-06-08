package product.marketplace.bestbuy.model

import play.api.libs.json.Json

case class ProductNames( 
    title : String
)

object ProductNames {
  implicit val formatter = Json.format[ProductNames]        
}
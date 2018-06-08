package product.marketplace.bestbuy.model

import play.api.libs.json.Json

case class MetadataContext(
    canonicalUrl : String
)

object MetadataContext {
  implicit val formatter = Json.format[MetadataContext]        
}
package product.marketplace.bestbuy.model

import play.api.libs.json.Json

case class MetadataResultSet(
    count : Int
)

object MetadataResultSet {
  implicit val formatter = Json.format[MetadataResultSet]        
}
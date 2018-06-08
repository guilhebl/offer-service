package product.marketplace.bestbuy.model

import play.api.libs.json.Json

case class Metadata (
    resultSet : MetadataResultSet,
    context : MetadataContext
)

object Metadata {
  implicit val formatter = Json.format[Metadata]        
}
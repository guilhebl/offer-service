package app.product.marketplace.bestbuy.model

import play.api.libs.json.Json

case class Descriptions(
    short : Option[String]
)

object Descriptions {
  implicit val formatter = Json.format[Descriptions]        
}
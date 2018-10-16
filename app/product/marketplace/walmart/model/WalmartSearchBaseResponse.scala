package product.marketplace.walmart.model

import play.api.libs.json._

case class WalmartSearchBaseResponse (items: Vector[WalmartSearchItem])

object WalmartSearchBaseResponse {
  implicit val formatter = Json.format[WalmartSearchBaseResponse]
}
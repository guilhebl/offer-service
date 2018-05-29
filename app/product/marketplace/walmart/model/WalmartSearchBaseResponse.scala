package app.product.marketplace.walmart.model

import play.api.libs.json._

case class WalmartSearchBaseResponse (items: Iterable[WalmartSearchItem])

object WalmartSearchBaseResponse {
  implicit val formatter = Json.format[WalmartSearchBaseResponse]
}
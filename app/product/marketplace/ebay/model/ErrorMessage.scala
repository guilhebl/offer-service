package app.product.marketplace.ebay.model

import play.api.libs.json._

case class ErrorMessage (
  error : Iterable[ErrorItem]
)

object ErrorMessage {
  implicit val formatter = Json.format[ErrorMessage]        
}

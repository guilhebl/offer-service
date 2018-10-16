package product.marketplace.ebay.model

import play.api.libs.json._

case class ErrorMessage (
  error : Vector[ErrorItem]
)

object ErrorMessage {
  implicit val formatter = Json.format[ErrorMessage]        
}

package product.marketplace.ebay.model

import play.api.libs.json._

case class ErrorItem (
  errorId : Iterable[String],
	domain : Iterable[String],
	severity : Iterable[String],
	category : Iterable[String],
	message : Iterable[String],
	subdomain : Iterable[String],
	parameter : Iterable[String]
)

object ErrorItem {
   implicit val documentFormatter = Json.format[ErrorItem]
}

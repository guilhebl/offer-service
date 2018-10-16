package product.marketplace.ebay.model

import play.api.libs.json._

case class ErrorItem (
  errorId : Vector[String],
	domain : Vector[String],
	severity : Vector[String],
	category : Vector[String],
	message : Vector[String],
	subdomain : Vector[String],
	parameter : Vector[String]
)

object ErrorItem {
   implicit val documentFormatter = Json.format[ErrorItem]
}

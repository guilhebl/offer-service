package app.product.marketplace.ebay.model

import play.api.libs.functional.syntax._
import play.api.libs.json.Format._
import play.api.libs.json.Reads._
import play.api.libs.json.Writes._
import play.api.libs.json._

case class ProductId (
    typeVal : String,
    value : String
)

object ProductId {
  /**
    * Mapping to and from JSON.
    */
  implicit val documentFormatter: Format[ProductId] = (
    (__ \ "@type").format[String] and
    (__ \ "__value__").format[String]
  ) (ProductId.apply, unlift(ProductId.unapply))
  
}

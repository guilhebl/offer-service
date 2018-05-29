package app.product.marketplace.ebay.model

import play.api.libs.functional.syntax._
import play.api.libs.json._

case class PriceInfo (
    currencyId : String,
    value : String
)

object PriceInfo {

  /**
    * Mapping to and from JSON.
    */
  implicit val documentFormatter: Format[PriceInfo] = (
    (__ \ "@currencyId").format[String] and
    (__ \ "__value__").format[String]
  ) (PriceInfo.apply, unlift(PriceInfo.unapply))
      
}


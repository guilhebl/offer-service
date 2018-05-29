package app.product.marketplace.ebay.model

import play.api.libs.json._

case class SellingStatus (
    currentPrice : Iterable[PriceInfo],
    convertedCurrentPrice : Iterable[PriceInfo],
    sellingState : Iterable[String],
    timeLeft : Iterable[String]
)

object SellingStatus {
  implicit val formatter = Json.format[SellingStatus]        
}

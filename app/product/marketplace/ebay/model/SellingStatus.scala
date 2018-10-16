package product.marketplace.ebay.model

import play.api.libs.json._

case class SellingStatus (
    currentPrice : Vector[PriceInfo],
    convertedCurrentPrice : Vector[PriceInfo],
    sellingState : Vector[String],
    timeLeft : Vector[String]
)

object SellingStatus {
  implicit val formatter = Json.format[SellingStatus]        
}

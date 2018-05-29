package app.product.marketplace.ebay.model

import play.api.libs.json._

case class ListingInfo (
    bestOfferEnabled : Iterable[String],
    buyItNowAvailable : Iterable[String],
    startTime : Iterable[String],
    endTime : Iterable[String],
    listingType : Iterable[String],
    gift : Iterable[String]
)

object ListingInfo {
  implicit val formatter = Json.format[ListingInfo]        
}

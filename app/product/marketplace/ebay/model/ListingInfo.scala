package product.marketplace.ebay.model

import play.api.libs.json._

case class ListingInfo (
    bestOfferEnabled : Vector[String],
    buyItNowAvailable : Vector[String],
    startTime : Vector[String],
    endTime : Vector[String],
    listingType : Vector[String],
    gift : Vector[String]
)

object ListingInfo {
  implicit val formatter = Json.format[ListingInfo]        
}

package product.model

import play.api.libs.json._

case class OfferDetailItem(
    partyName: String,
    semanticName: String,
    partyImageFileUrl: String,    
    price: Double,
    rating: Float,
    numReviews: Int     
)

object OfferDetailItem {

  /**
    * Mapping to and from JSON.
    */
  implicit val formatter = Json.format[OfferDetailItem]
}
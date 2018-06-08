package product.marketplace.walmart.model

import play.api.libs.json._

case class WalmartSpecialOfferSearchResponse(
  specialOffer: String,
  format: String,
  nextPage: String,
  items: Iterable[WalmartSearchItem]
)

object WalmartSpecialOfferSearchResponse {
      implicit val formatter = Json.format[WalmartSpecialOfferSearchResponse]
}
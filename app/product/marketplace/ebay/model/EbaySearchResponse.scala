package app.product.marketplace.ebay.model

import play.api.libs.json._

case class EbaySearchResponse (
  findItemsByKeywordsResponse: Iterable[EbayFindingServiceResponse]
)

object EbaySearchResponse {
  implicit val formatter = Json.format[EbaySearchResponse]        
}

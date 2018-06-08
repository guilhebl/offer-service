package product.marketplace.ebay.model

import play.api.libs.json.Json

case class EbayProductDetailResponse (findItemsByProductResponse: Iterable[EbayFindingServiceResponse])

object EbayProductDetailResponse {
  implicit val formatter = Json.format[EbayProductDetailResponse]
}

package product.marketplace.bestbuy.model

import play.api.libs.json.Json

case class BestBuyTrendingResponse(
    metadata : Metadata,
    results : Iterable[ProductSpecialOfferItem]
)

object BestBuyTrendingResponse {
  implicit val formatter = Json.format[BestBuyTrendingResponse]        
}
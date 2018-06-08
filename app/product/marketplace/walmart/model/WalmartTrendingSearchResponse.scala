package product.marketplace.walmart.model

import java.util.Date

import play.api.libs.json._

case class WalmartTrendingSearchResponse (
 time: Date,
 items: Iterable[WalmartSearchItem]
)

object WalmartTrendingSearchResponse {
  implicit val jsonFormatter = Json.format[WalmartTrendingSearchResponse]
}

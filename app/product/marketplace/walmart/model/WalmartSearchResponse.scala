package app.product.marketplace.walmart.model

import play.api.libs.json._

case class WalmartSearchResponse (
  query: String,  
  sort: String,  
  responseGroup: String,  
  totalResults: Int,  
  start: Int, 
  numItems: Int,
  items: Iterable[WalmartSearchItem]
)

object WalmartSearchResponse {
      implicit val formatter = Json.format[WalmartSearchResponse]
}
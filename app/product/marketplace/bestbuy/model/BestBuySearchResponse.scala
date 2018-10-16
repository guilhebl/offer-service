package product.marketplace.bestbuy.model

import play.api.libs.json._

case class BestBuySearchResponse(
    from : Int,
    to : Int,
    total : Int,    
    currentPage : Int,
    totalPages : Int,
    queryTime : String,
    totalTime : String,
    partial : Boolean,
    canonicalUrl : String,
    products : Vector[ProductItem]
)

object BestBuySearchResponse {
  implicit val formatter = Json.format[BestBuySearchResponse]        
}
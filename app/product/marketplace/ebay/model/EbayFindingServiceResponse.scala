package product.marketplace.ebay.model

import play.api.libs.json._

case class EbayFindingServiceResponse (
    ack : Vector[String], 
    version : Vector[String],
    timestamp : Vector[String],
    itemSearchURL : Option[Vector[String]],
    searchResult : Option[Vector[SearchResult]],
    paginationOutput : Option[Vector[PaginationOutput]],
    errorMessage : Option[Vector[ErrorMessage]]
)

object EbayFindingServiceResponse {
  implicit val formatter = Json.format[EbayFindingServiceResponse]        
}

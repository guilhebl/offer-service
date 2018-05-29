package app.product.marketplace.ebay.model

import play.api.libs.json._

case class EbayFindingServiceResponse (
    ack : Iterable[String], 
    version : Iterable[String],
    timestamp : Iterable[String],
    itemSearchURL : Option[Iterable[String]],
    searchResult : Option[Iterable[SearchResult]],
    paginationOutput : Option[Iterable[PaginationOutput]],
    errorMessage : Option[Iterable[ErrorMessage]]
)

object EbayFindingServiceResponse {
  implicit val formatter = Json.format[EbayFindingServiceResponse]        
}

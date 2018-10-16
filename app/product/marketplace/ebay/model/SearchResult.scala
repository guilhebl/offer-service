package product.marketplace.ebay.model

import play.api.libs.functional.syntax._
import play.api.libs.json._

case class SearchResult (
 count : String,
 item : Vector[SearchResultItem]
)

object SearchResult {
  /**
    * Mapping to and from JSON.
    */
  implicit val documentFormatter: Format[SearchResult] = (
    (__ \ "@count").format[String] and
    (__ \ "item").format[Vector[SearchResultItem]]
  ) (SearchResult.apply, unlift(SearchResult.unapply))
        
}

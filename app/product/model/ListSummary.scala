package product.model

import play.api.libs.json._

case class ListSummary (  
    page: Int,
    pageCount: Int,
    totalCount: Int
)

object ListSummary {

  /**
    * Mapping to and from JSON.
    */
  implicit val formatter = Json.format[ListSummary]
}
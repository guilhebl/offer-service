package product.marketplace.ebay.model

import play.api.libs.json._

case class PaginationOutput (
    pageNumber : Vector[String],
    entriesPerPage : Vector[String],
    totalPages : Vector[String],
    totalEntries : Vector[String]
)

object PaginationOutput {
 implicit val documentFormatter = Json.format[PaginationOutput]
}

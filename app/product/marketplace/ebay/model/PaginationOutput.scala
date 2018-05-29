package app.product.marketplace.ebay.model

import play.api.libs.json._

case class PaginationOutput (
    pageNumber : Iterable[String],
    entriesPerPage : Iterable[String],
    totalPages : Iterable[String],
    totalEntries : Iterable[String]
)

object PaginationOutput {
 implicit val documentFormatter = Json.format[PaginationOutput]
}

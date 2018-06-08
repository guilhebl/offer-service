package product.model

import play.api.libs.json._

case class OfferList (
  list: Iterable[Offer],
  summary: ListSummary
)

object OfferList {

  /**
    * Mapping to and from JSON.
    */
  implicit val formatter = Json.format[OfferList]
}

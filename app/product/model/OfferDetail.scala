package product.model

import play.api.libs.json._

case class OfferDetail (
    offer: Offer,
    description: String,
    attributes: Iterable[NameValue],
    productDetailItems: Iterable[OfferDetailItem]
)

object OfferDetail {

  /**
    * Mapping to and from JSON.
    */
  implicit val documentFormatter = Json.format[OfferDetail]
}
package product.model

import play.api.libs.json._

case class OfferDetail (
    offer: Offer,
    description: String,
    attributes: Vector[NameValue],
    productDetailItems: Vector[OfferDetailItem],
    lastOfferLog: Option[OfferLog] = None
)

object OfferDetail {

  /**
    * Mapping to and from JSON.
    */
  implicit val documentFormatter = Json.format[OfferDetail]
}
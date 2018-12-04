package product.model

import play.api.libs.json._

/**
  * A wrapper for an offer detail containing extra elements such as
  *
  * constitutes a Decorator pattern it Decorates a simple Offer Detail with additional info such as
  * adding the Analytics summary of Views for this UPC
  */
case class OfferDetailResponse (offerDetail: OfferDetail)

object OfferDetailResponse {
  implicit val documentFormatter = Json.format[OfferDetailResponse]
}
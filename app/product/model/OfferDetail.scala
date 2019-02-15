package product.model

import play.api.libs.json._

case class OfferDetail (
    offer: Offer,
    description: String,
    attributes: Vector[NameValue],
    productDetailItems: Vector[OfferDetailItem],
    priceLogs: Vector[OfferPriceLog] = Vector.empty[OfferPriceLog]
)

object OfferDetail {

  /**
    * Mapping to and from JSON.
    */
  implicit val documentFormatter = Json.format[OfferDetail]

  /**
  * Creates a cache key representation in REDIS Cache
  **/
  def buildCacheKey(id:String): String = s"OfferDetail$id"

  def mergeOption(response: Option[OfferDetail], response2: Option[OfferDetail]): Option[OfferDetail] = {
    if (response.isEmpty) {
      None
    } else if (response2.isEmpty) {
      response
    } else {
      val item1 = response.get
      val item = response2.get

      // return a new merged OfferDetail obj.
      Some(
        OfferDetail(
          item1.offer,
          item1.description,
          item1.attributes,
          item1.productDetailItems ++ Seq(
            new OfferDetailItem(
              item.offer.partyName,
              item.offer.semanticName,
              item.offer.partyImageFileUrl,
              item.offer.price,
              item.offer.rating,
              item.offer.numReviews
            )
          ),
          Vector.empty[OfferPriceLog]
        )
      )
    }
  }

}
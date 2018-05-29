package app.product.model

import play.api.libs.json._

case class Offer(
    id: String,
    upc: Option[String],
    name: String,
    partyName: String,
    semanticName: String,
    mainImageFileUrl: String,    
    partyImageFileUrl: String,    
    price: Double,
    productCategory: String,
    rating: Float,
    numReviews: Int     
)

object Offer {

  /**
    * Mapping to and from JSON.
    */
  implicit val documentFormatter = Json.format[Offer]

}
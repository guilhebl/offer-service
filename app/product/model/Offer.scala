package product.model

import play.api.libs.json._

import scala.language.implicitConversions

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
  implicit val documentFormatter = Json.format[Offer]
}

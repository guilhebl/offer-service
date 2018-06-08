package product.model

import org.mongodb.scala.Document
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

  implicit def documentToEntityConverterMongo(doc: Document): Offer = {
    Offer(
      doc.get("id").get.asString().getValue,
      Some(doc.get("upc").get.asString().getValue),
      doc.get("name").get.asString().getValue,
      doc.get("partyName").get.asString().getValue,
      doc.get("semanticName").get.asString().getValue,
      doc.get("mainImageFileUrl").get.asString().getValue,
      doc.get("partyImageFileUrl").get.asString().getValue,
      doc.get("price").get.asDouble().getValue,
      doc.get("productCategory").get.asString().getValue,
      doc.get("rating").get.asDouble().getValue.floatValue,
      doc.get("numReviews").get.asInt32().getValue
    )
  }

  implicit def entityToDocumentConverterMongo(e: Offer): Document = {
    Document(
      "id" -> e.id,
      "upc" -> e.upc.getOrElse(""),
      "name" -> e.name,
      "partyName" -> e.partyName,
      "semanticName" -> e.semanticName,
      "mainImageFileUrl" -> e.mainImageFileUrl,
      "partyImageFileUrl" -> e.partyImageFileUrl,
      "price" -> e.price,
      "productCategory" -> e.productCategory,
      "rating" -> e.rating.toDouble,
      "numReviews" -> e.numReviews
    )
  }

}

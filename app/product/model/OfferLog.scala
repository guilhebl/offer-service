package product.model

import org.mongodb.scala.Document
import play.api.libs.json.Json

/**
  * Logs a particular offer at a specific point of time
  */
case class OfferLog(offer: Offer, timestamp: Long)

object OfferLog {

  implicit val documentFormatter = Json.format[OfferLog]

  implicit def documentToEntityConverterMongo(doc: Document): OfferLog = {
    OfferLog(
      Offer.documentToEntityConverterMongo(doc.get("offer").get.asDocument()),
      doc.get("timestamp").get.asInt64().getValue
    )
  }

  implicit def entityToDocumentConverterMongo(e: OfferLog): Document = {
    Document(
      "offer" -> Offer.entityToDocumentConverterMongo(e.offer),
      "timestamp" -> e.timestamp
    )
  }

}

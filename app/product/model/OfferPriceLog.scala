package product.model

import play.api.libs.json.Json

/**
  * Logs a price offered from a particular source in a point in time
  */
case class OfferPriceLog(upc: String, source: String, price: Double, timestamp: Long)

object OfferPriceLog {
  implicit val documentFormatter = Json.format[OfferPriceLog]
}

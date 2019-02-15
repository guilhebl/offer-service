package product.model

import java.time.Instant

import play.api.libs.json.Json

/**
  * Logs a price offered from a particular source in a point in time
  */
case class OfferPriceLog(upc: String, source: String, price: Double, timestamp: Instant)

object OfferPriceLog {
  implicit val documentFormatter = Json.format[OfferPriceLog]
}

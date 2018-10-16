package product.model

import play.api.libs.json.Json

/**
  * Logs a particular offer at a specific point of time
  */
case class OfferLog(upc: String, offer: Offer, timestamp: Long, totalViews: Long, currentDayViews: Long, currentMonthViews: Long)

object OfferLog {
  implicit val documentFormatter = Json.format[OfferLog]
}

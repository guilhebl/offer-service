package product.model
import java.time.Instant

import play.api.libs.json.{Json, OFormat}

case class ProductTracking(upc: String, created: Instant, lastSeen: Instant, active: Boolean)

object ProductTracking {
  implicit val documentFormatter: OFormat[ProductTracking] = Json.format[ProductTracking]
}



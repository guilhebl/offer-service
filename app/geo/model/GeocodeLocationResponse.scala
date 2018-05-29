package geo.model

import play.api.libs.functional.syntax._
import play.api.libs.json._

case class GeocodeLocationResponse (
   results: Vector[Place],
   status: String
)

object GeocodeLocationResponse {
    
  implicit val documentFormatter: Format[GeocodeLocationResponse] = (
    (__ \ "results").format[Vector[Place]] and
    (__ \ "status").format[String]
  ) (GeocodeLocationResponse.apply, unlift(GeocodeLocationResponse.unapply))
      
}
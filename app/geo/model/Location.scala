package geo.model

import play.api.libs.functional.syntax._
import play.api.libs.json._

case class Location (
  lat: Double,
	lng: Double
)

object Location {
    
  implicit val documentFormatter: Format[Location] = (
    (__ \ "lat").format[Double] and
    (__ \ "lng").format[Double]  
  ) (Location.apply, unlift(Location.unapply))
      
}
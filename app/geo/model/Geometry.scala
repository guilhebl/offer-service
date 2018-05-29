package geo.model

import play.api.libs.functional.syntax._
import play.api.libs.json._

case class Geometry (
  locationType: String,
  location: Location,
  viewport: ViewPort
)

object Geometry {
    
  implicit val documentFormatter: Format[Geometry] = (
    (__ \ "location_type").format[String] and
    (__ \ "location").format[Location] and
    (__ \ "viewport").format[ViewPort]  
  ) (Geometry.apply, unlift(Geometry.unapply))
      
}
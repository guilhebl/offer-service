package geo.model

import play.api.libs.functional.syntax._
import play.api.libs.json._

case class Place(
    placeId:String,
    formattedAddress:String,
    addressComponents: Vector[AddressComponent],
    types: Vector[String],
    geometry: Geometry
)

object Place {
    
  implicit val documentFormatter: Format[Place] = (
    (__ \ "place_id").format[String] and
    (__ \ "formatted_address").format[String] and
    (__ \ "address_components").format[Vector[AddressComponent]] and
    (__ \ "types").format[Vector[String]] and
    (__ \ "geometry").format[Geometry]  
  ) (Place.apply, unlift(Place.unapply))
      
}
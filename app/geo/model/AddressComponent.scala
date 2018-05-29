package geo.model

import play.api.libs.functional.syntax._
import play.api.libs.json._

case class AddressComponent(
 longName:String,
 shortName:String,
 types: Vector[String]
)

object AddressComponent {
    
  implicit val documentFormatter: Format[AddressComponent] = (
    (__ \ "long_name").format[String] and
    (__ \ "short_name").format[String] and
    (__ \ "types").format[Vector[String]]  
  ) (AddressComponent.apply, unlift(AddressComponent.unapply))
      
}
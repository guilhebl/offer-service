package geo.model

import play.api.libs.functional.syntax._
import play.api.libs.json._

case class ViewPort (
  northeast:Location,
  southwest:Location
)

object ViewPort {
    
  implicit val documentFormatter: Format[ViewPort] = (
    (__ \ "northeast").format[Location] and
    (__ \ "southwest").format[Location]  
  ) (ViewPort.apply, unlift(ViewPort.unapply))
      
}
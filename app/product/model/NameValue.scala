package app.product.model

import play.api.libs.json._

case class NameValue (  
    name: String,
    value: String
)

object NameValue {
  implicit val jsonFormatter = Json.format[NameValue]
}
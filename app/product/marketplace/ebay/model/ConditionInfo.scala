package product.marketplace.ebay.model

import play.api.libs.json._

case class ConditionInfo (
    conditionId : Vector[String],
    conditionDisplayName : Vector[String]
)

object ConditionInfo {

  /**
    * Mapping to and from JSON.
    */
  implicit val documentFormatter = Json.format[ConditionInfo]
      
}

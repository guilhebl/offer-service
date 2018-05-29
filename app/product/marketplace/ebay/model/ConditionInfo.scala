package app.product.marketplace.ebay.model

import play.api.libs.json._

case class ConditionInfo (
    conditionId : Iterable[String],
    conditionDisplayName : Iterable[String]
)

object ConditionInfo {

  /**
    * Mapping to and from JSON.
    */
  implicit val documentFormatter = Json.format[ConditionInfo]
      
}

package product.marketplace.ebay.model

import play.api.libs.json._

case class ShippingInfo (
    shippingServiceCost : Option[Vector[PriceInfo]],
    shippingType : Vector[String],
    shipToLocations : Vector[String],
    expeditedShipping : Vector[String],
    oneDayShippingAvailable : Vector[String],
    handlingTime : Vector[String]
)

object ShippingInfo {
 implicit val documentFormatter = Json.format[ShippingInfo]
}

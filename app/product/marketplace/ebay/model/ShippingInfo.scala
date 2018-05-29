package app.product.marketplace.ebay.model

import play.api.libs.json._

case class ShippingInfo (
    shippingServiceCost : Option[Iterable[PriceInfo]],
    shippingType : Iterable[String],
    shipToLocations : Iterable[String],
    expeditedShipping : Iterable[String],
    oneDayShippingAvailable : Iterable[String],
    handlingTime : Iterable[String]
)

object ShippingInfo {
 implicit val documentFormatter = Json.format[ShippingInfo]
}

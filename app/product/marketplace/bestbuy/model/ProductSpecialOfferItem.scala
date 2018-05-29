package app.product.marketplace.bestbuy.model

import play.api.libs.json.Json

case class ProductSpecialOfferItem(
    customerReviews : CustomerReviews,
    descriptions : Descriptions,
    images : ProductImages,
    links : ProductLinks,
    names : ProductNames,
    prices : ProductPrices,
    rank : Int,
    sku : String
)

object ProductSpecialOfferItem {
  implicit val formatter = Json.format[ProductSpecialOfferItem]        
}
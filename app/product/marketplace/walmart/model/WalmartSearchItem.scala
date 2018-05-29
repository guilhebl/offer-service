package app.product.marketplace.walmart.model

import play.api.libs.json._

case class WalmartSearchItem (
  itemId: Int,
  parentItemId: Int,
  name: String,
  salePrice: Option[Double],
  upc: Option[String],
  categoryPath: String,
  shortDescription: Option[String],
  longDescription: Option[String],
  thumbnailImage: Option[String],
  mediumImage: Option[String],
  largeImage: Option[String],
  productTrackingUrl: String,
  modelNumber: Option[String],
  productUrl: String,
  customerRating: Option[String],
  numReviews: Option[Int],
  categoryNode: String
)

object WalmartSearchItem {
  implicit val formatter = Json.format[WalmartSearchItem]        
}
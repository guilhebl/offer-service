package product.marketplace.walmart.model

import play.api.libs.json._

case class ImageEntity (
  thumbnailImage: String,
  mediumImage: String,
  largeImage: String,
  entityType: String
)

object ImageEntity {
  implicit val formatter = Json.format[ImageEntity]
}

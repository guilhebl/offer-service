package product.marketplace.walmart.model

import play.api.libs.json._

case class GiftOptions (
  allowGiftWrap: Boolean,
  allowGiftMessage: Boolean,
  allowGiftReceipt: Boolean
)

object GiftOptions {
  implicit val formatter = Json.format[GiftOptions]
}

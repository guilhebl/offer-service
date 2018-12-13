package product.marketplace.ebay.model

import play.api.libs.json._

case class SearchResultItem (
    productId : Option[Vector[ProductId]],
    itemId : Vector[String],
    title : Vector[String],
    globalId : Vector[String],
    galleryURL : Vector[String],
    viewItemURL : Vector[String],
    paymentMethod : Option[Vector[String]],
    postalCode : Option[Vector[String]],
    location : Vector[String],
    country : Vector[String],
    primaryCategory : Vector[PrimaryCategory],
    shippingInfo : Vector[ShippingInfo],
    sellingStatus : Vector[SellingStatus],
    listingInfo : Vector[ListingInfo],
    returnsAccepted : Vector[String],
    pictureURLLarge : Option[Vector[String]],
    condition : Vector[ConditionInfo],
    isMultiVariationListing : Vector[String],
    topRatedListing : Vector[String]
)

object SearchResultItem {
  implicit val formatter = Json.format[SearchResultItem]        
}

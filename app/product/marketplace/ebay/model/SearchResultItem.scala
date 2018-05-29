package app.product.marketplace.ebay.model

import play.api.libs.json._

case class SearchResultItem (
    productId : Option[Iterable[ProductId]],
    itemId : Iterable[String],
    title : Iterable[String],
    globalId : Iterable[String],
    galleryURL : Iterable[String],
    viewItemURL : Iterable[String],
    paymentMethod : Iterable[String],
    postalCode : Option[Iterable[String]],
    location : Iterable[String],
    country : Iterable[String],
    primaryCategory : Iterable[PrimaryCategory],
    shippingInfo : Iterable[ShippingInfo],
    sellingStatus : Iterable[SellingStatus],
    listingInfo : Iterable[ListingInfo],
    returnsAccepted : Iterable[String],
    pictureURLLarge : Option[Iterable[String]],
    condition : Iterable[ConditionInfo],
    isMultiVariationListing : Iterable[String],
    topRatedListing : Iterable[String]
)

object SearchResultItem {
  implicit val formatter = Json.format[SearchResultItem]        
}

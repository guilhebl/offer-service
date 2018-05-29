package app.product.marketplace.bestbuy.model

import play.api.libs.json.Json

case class CustomerReviews(
    averageScore : Option[Float],
    count : Option[Int]
)

object CustomerReviews {
  implicit val formatter = Json.format[CustomerReviews]        
}
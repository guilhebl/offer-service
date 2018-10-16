package product.model

import play.api.libs.json._

case class OfferList (
  list: Vector[Offer],
  summary: ListSummary
)

object OfferList {

  /**
    * Mapping to and from JSON.
    */
  implicit val formatter = Json.format[OfferList]

  def empty(): OfferList = {
    OfferList(
      Vector.empty[Offer],
      ListSummary(
        page = 1,
        pageCount = 0,
        totalCount = 0
      )
    )
  }

  /**
  * Merge 2 options
    *
    * @param acc accumulator
    * @param item new item
    * @return
    */
  def merge(acc: Option[OfferList], item: Option[OfferList]): Option[OfferList] = {
    if (acc.isEmpty) return None
    if (item.isEmpty) return acc

    val page = if (item.get.summary.page > acc.get.summary.page) item.get.summary.page else acc.get.summary.page

    Some(
      new OfferList(
        acc.get.list ++ item.get.list,
        new ListSummary(
          page,
          item.get.summary.pageCount,
          acc.get.summary.totalCount + item.get.summary.totalCount
        )
      )
    )
  }
}

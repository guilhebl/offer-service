package product.model

import common.util.EntityValidationUtil.isValidCountry
import play.api.libs.json._
import product.marketplace.common.MarketplaceConstants._

case class ListRequest(
    searchColumns : Vector[NameValue] = Vector.empty,
    sortColumn : Option[String] = None,
    sortOrder : Option[String] = None,
	  page : Option[Int] = None,
	  rowsPerPage : Option[Int] = None
)

object ListRequest {
  implicit val formatter = Json.format[ListRequest]

  def buildFromQuery(q: String): ListRequest = {
    ListRequest(searchColumns = Vector(
      NameValue(Name, q)
    ))
  }

  /**
    * Returns a copy of this request with page as param
    * @return
    */
  def fromPage(currentRequest: ListRequest, page: Int): ListRequest = {
    ListRequest(
      currentRequest.searchColumns,
      currentRequest.sortColumn,
      currentRequest.sortOrder,
      Some(page),
      currentRequest.rowsPerPage
    )
  }

  /**
    * Returns a copy of this request with keyword as param
    * @return
    */
  def fromKeyword(currentRequest: ListRequest, keyword: String): ListRequest = {
    ListRequest(
      Vector(NameValue(Name, keyword)),
      currentRequest.sortColumn,
      currentRequest.sortOrder,
      currentRequest.page,
      currentRequest.rowsPerPage
    )
  }

  /**
    * Transforms ListRequest in a Map of params
    * @param req params
    * @return
    */
  def filterParams(req: ListRequest): Map[String, String] = {
    req.searchColumns.filter(_.value.nonEmpty).map(x => (x.name, x.value)).toMap
  }

  /**
    * Filter country or defaults to USA
    * @param params country if present
    * @return
    */
  def filterCountry(params: Map[String, String]): String = {
    if (params.contains(Country) && isValidCountry(params(Country))) params(Country) else UnitedStates
  }

  def filterCountry(req: ListRequest): String = {
    val params = filterParams(req)
    filterCountry(params)
  }

}

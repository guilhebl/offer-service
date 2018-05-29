package app.product.model

import play.api.libs.json._

case class ListRequest(
    searchColumns : Seq[NameValue] = Seq.empty,
    sortColumn : Option[String] = None,
    sortOrder : Option[String] = None,
	  page : Option[Int] = None,
	  rowsPerPage : Option[Int] = None
)

object ListRequest {
  implicit val formatter = Json.format[ListRequest]  
}

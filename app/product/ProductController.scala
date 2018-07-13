package product

import javax.inject.Inject
import play.api.data.Form
import play.api.libs.json.Json
import play.api.mvc._
import product.model.{ListRequest, NameValue}

import scala.concurrent.{ExecutionContext, Future}

/**
  * Takes HTTP requests and produces JSON.
  */
class ProductController @Inject()(cc: ProductControllerComponents)(implicit ec: ExecutionContext)
    extends ProductBaseController(cc) {

  private val searchRequest: Form[ListRequest] = {
    import play.api.data.Forms._
    Form(
      mapping(
        "searchColumns" -> seq(
            mapping(
                "name" -> nonEmptyText, 
                "value" -> nonEmptyText
            )(NameValue.apply)(NameValue.unapply)
         ),
        "sortColumn" -> optional(text),
        "sortOrder" -> optional(text),
        "page" -> optional(number),
        "rowsPerPage" -> optional(number)
      )(ListRequest.apply)(ListRequest.unapply)
    )
  }

  def search(): Action[AnyContent] = ProductAction.async { implicit request =>
    processJsonPostList()
  }

  implicit class RichResult (result: Result) {
    def enableCors =  result.withHeaders(
      "Access-Control-Allow-Origin" -> "http://localhost:4200"
    )
  }

  def searchOffers(query: String): Action[AnyContent] = ProductAction.async { implicit request =>
    productResourceHandler.search(ListRequest.buildFromQuery(query)).map { offerList =>
      Ok(Json.toJson(offerList)).enableCors
    }
  }

  def get(id: String, idType: String, source: String, country: Option[String]): Action[AnyContent] = ProductAction.async { implicit request =>
    productResourceHandler.get(id, idType, source, country).map { product =>
      Ok(Json.toJson(product))
    }
  }

  /**
   * Process the JSON post for List request
   */
  private def processJsonPostList[A]()(implicit request: ProductRequest[A]): Future[Result] = {
    def failure(badForm: Form[ListRequest]) = {
      Future.successful(BadRequest(badForm.errorsAsJson))
    }

    def success(input: ListRequest) = {
      productResourceHandler.search(input).map { offerList =>
        Ok(Json.toJson(offerList))
      }
    }
    searchRequest.bindFromRequest().fold(failure, success)
  }
}

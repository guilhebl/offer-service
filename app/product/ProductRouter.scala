package product

import javax.inject.Inject
import play.api.routing.Router.Routes
import play.api.routing.SimpleRouter
import play.api.routing.sird._

/**
  * Routes and URLs to the ProductResource controller.
  */
class ProductRouter @Inject()(controller: ProductController) extends SimpleRouter {
  val prefix = "/api/v1/products"

  def link(id: String): String = {
    import com.netaporter.uri.dsl._
    val url = prefix / id
    url.toString()
  }

  override def routes: Routes = {
    case POST(p"/") =>
      controller.search

    case GET(p"/" ? q"q=$query") =>
      controller.searchOffers(query)

    case GET(p"/$id" ? q"idType=$idType" & q"source=$source" & q_o"country=$country") =>
      controller.get(id, idType, source, country)
  }

}

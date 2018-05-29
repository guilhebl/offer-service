package geo

import javax.inject.Inject
import play.api.routing.Router.Routes
import play.api.routing.SimpleRouter
import play.api.routing.sird._

/**
  * Routes and URLs to the GeolocationResource controller.
  */
class GeolocationRouter @Inject()(controller: GeolocationController) extends SimpleRouter {
  val prefix = "/geo"

  def link(id: String): String = {
    import com.netaporter.uri.dsl._
    val url = prefix / id
    url.toString()
  }

  override def routes: Routes = {
    case GET(p"/location/$latlng") =>
      controller.location(latlng)      
  }

}

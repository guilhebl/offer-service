package geo

import javax.inject.Inject
import play.api.Logger
import play.api.libs.json.Json
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}

/**
  * Takes HTTP requests and produces JSON.
  */
class GeolocationController @Inject()(cc: GeolocationControllerComponents)(implicit ec: ExecutionContext)
    extends GeolocationBaseController(cc) {

  private val logger = Logger(getClass)
  
  private val regex = """^(\-?\d+(\.\d+)?),\s*(\-?\d+(\.\d+)?)$""".r

  def location(latlng:String): Action[AnyContent] = GeolocationAction.async { implicit request =>
    logger.trace("index: ")
    
    def f = {
      val parts = latlng.split(",")            
      geolocationResourceHandler.getLocation(parts(0), parts(1)).map { r =>
          Ok(Json.toJson(r))
      }     
    }

    latlng match {
      case regex(_*) => f
      case _ => Future.successful(BadRequest("error"))
    }     
  }
  
}

package geo

import geo.model._
import javax.inject.Inject
import play.api.MarkerContext

import scala.concurrent.{ExecutionContext, Future}

/**
  * Controls access to the backend data
  */
class GeolocationResourceHandler @Inject()(geolocationRepository: GeolocationRepository) (implicit ec: ExecutionContext) {
   def getLocation(lat:String, lng:String)(implicit mc: MarkerContext): Future[Option[GeocodeLocationResponse]] = {
     geolocationRepository.getLocation(lat, lng)     
   }
}


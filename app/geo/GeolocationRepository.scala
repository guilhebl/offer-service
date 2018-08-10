package geo

import common.config.AppConfigService
import common.executor.RepositoryDispatcherContext
import geo.model.GeocodeLocationResponse
import javax.inject.{Inject, Singleton}
import play.api.libs.json._
import play.api.libs.ws._
import play.api.{Logger, MarkerContext}

import scala.concurrent.Future
import scala.concurrent.duration._

/**
  * A pure non-blocking interface for the GeolocationRepository.
  */
trait GeolocationRepository {  
  def getLocation(lat:String, lng:String)(implicit mc: MarkerContext): Future[Option[GeocodeLocationResponse]] 
}

/**
  * A custom execution context is used here to establish that blocking operations should be
  * executed in a different thread than Play's ExecutionContext, which is used for CPU bound tasks
  * such as rendering.
  * 
  */
@Singleton
class GeolocationRepositoryImpl @Inject()(ws: WSClient, appConfigService: AppConfigService)
                                         (implicit ec: RepositoryDispatcherContext) extends GeolocationRepository {
  
  private val logger = Logger(this.getClass)
   
  def getLocation(lat:String, lng:String)(implicit mc: MarkerContext): Future[Option[GeocodeLocationResponse]] = {
    
    val timeout:String = appConfigService.properties("timeoutExternalAPI")
		val apiKey:String = appConfigService.properties("googleMapsAPIKey")
		val endpoint:String = appConfigService.properties("googleMapsEndpoint")
		val path:String = appConfigService.properties("googleMapsGeolocationPath")
		val url = endpoint + path
		val latlng = lat + "," + lng

		val futureResult: Future[Option[GeocodeLocationResponse]] = ws.url(url)
		.addHttpHeaders("Accept" -> "application/json")    
    .addQueryStringParameters("latlng" -> latlng)
    .addQueryStringParameters("sensor" -> "false")
    .addQueryStringParameters("key" -> apiKey)
    .withRequestTimeout(timeout.toInt.millis)
		.get()
		.map {
        response => {
          val resp = response.json.validate[GeocodeLocationResponse]
          resp match {
            case s: JsSuccess[GeocodeLocationResponse] => Some(s.get)
            case e: JsError =>
                logger.trace("Errors: " + JsError.toJson(e).toString()) 
                None
          }
        }
    }
		futureResult
  }
  
}

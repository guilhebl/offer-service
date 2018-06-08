package product

import common.log.RequestMarkerContext
import javax.inject.Inject
import play.api.http.{FileMimeTypes, HttpVerbs}
import play.api.i18n.{Langs, MessagesApi}
import play.api.mvc._
import play.api.{Logger, MarkerContext}

import scala.concurrent.{ExecutionContext, Future}

/**
  * A wrapped request for product resources.
  *
  * This is commonly used to hold request-specific information like
  * security credentials, and useful shortcut methods.
  */
trait ProductRequestHeader extends MessagesRequestHeader with PreferredMessagesProvider
class ProductRequest[A](request: Request[A], val messagesApi: MessagesApi) extends WrappedRequest(request) with ProductRequestHeader

/**
  * The action builder for the Product resource.
  *
  * This is the place to put logging, metrics, to augment
  * the request with contextual data, and manipulate the
  * result.
  */
class ProductActionBuilder @Inject()(messagesApi: MessagesApi, playBodyParsers: PlayBodyParsers)
                                 (implicit val executionContext: ExecutionContext)
    extends ActionBuilder[ProductRequest, AnyContent]
    with RequestMarkerContext
    with HttpVerbs {

  val parser: BodyParser[AnyContent] = playBodyParsers.anyContent

  type ProductRequestBlock[A] = ProductRequest[A] => Future[Result]

  private val logger = Logger(this.getClass)

  override def invokeBlock[A](request: Request[A],
                              block: ProductRequestBlock[A]): Future[Result] = {
    // Convert to marker context and use request in block
    implicit val markerContext: MarkerContext = requestHeaderToMarkerContext(request)
    logger.trace(s"invokeBlock: ")

    val future = block(new ProductRequest(request, messagesApi))

    future.map { result =>
      request.method match {
        case GET | HEAD =>
          result.withHeaders("Cache-Control" -> s"max-age: 100")
        case _ =>
          result
      }
    }
  }
}

/**
 * Packages up the component dependencies for the product controller.
 *
 * This is a good way to minimize the surface area exposed to the controller, so the
 * controller only has to have one thing injected.
 */
case class ProductControllerComponents @Inject()(productActionBuilder: ProductActionBuilder,
                                               productResourceHandler: ProductResourceHandler,
                                               actionBuilder: DefaultActionBuilder,
                                               parsers: PlayBodyParsers,
                                               messagesApi: MessagesApi,
                                               langs: Langs,
                                               fileMimeTypes: FileMimeTypes,
                                               executionContext: scala.concurrent.ExecutionContext)
  extends ControllerComponents

/**
 * Exposes actions and handler to the ProductController by wiring the injected state into the base class.
 */
class ProductBaseController @Inject()(pcc: ProductControllerComponents) extends BaseController with RequestMarkerContext {
  override protected def controllerComponents: ControllerComponents = pcc

  def ProductAction: ProductActionBuilder = pcc.productActionBuilder

  def productResourceHandler: ProductResourceHandler = pcc.productResourceHandler
}
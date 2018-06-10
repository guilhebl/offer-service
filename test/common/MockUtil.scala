package common

import akka.actor
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import common.executor.{RepositoryDispatcherContext, WorkerDispatcherContext}
import play.api.libs.json.Json
import play.api.mvc.{DefaultActionBuilder, PlayBodyParsers}
import product.model.{OfferDetail, OfferList}

import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext}
import scala.io.Source

object MockBaseUtil {

  val MockFilesPath = "test/resources/mock/product"
  val MockMarketplaceFilesPath = s"$MockFilesPath/marketplace"
  private val actorSystem: ActorSystem = actor.ActorSystem("unit-testing")
  implicit val materializer: ActorMaterializer = ActorMaterializer()(actorSystem)
  val bodyParser: PlayBodyParsers = PlayBodyParsers()
  val actionBuilder: DefaultActionBuilder = DefaultActionBuilder(bodyParser.anyContent)

  sys addShutdownHook {
    materializer.shutdown()
    Await.result(actorSystem.terminate(), Duration.Inf)
  }

  /* MOCK data */
  private val userDir = System.getProperty("user.dir")
  private val productList = Json.parse(Source.fromFile(s"$MockFilesPath/sample_offer.json").getLines.mkString).as[OfferList]
  private val offerDetail = Json.parse(Source.fromFile(s"$MockFilesPath/sample_offer_detail.json").getLines.mkString).as[OfferDetail]
  private val offerDetailNoItems = Json.parse(Source.fromFile(s"$MockFilesPath/sample_offer_detail_no_detail_items.json").getLines.mkString).as[OfferDetail]
  private val offerDetailNoItemsNoUpc = Json.parse(Source.fromFile(s"$MockFilesPath/sample_offer_detail_no_upc_no_items.json").getLines.mkString).as[OfferDetail]
  private val offerDetailNoItemsParty2 = Json.parse(Source.fromFile(s"$MockFilesPath/sample_offer_detail_no_items_party2.json").getLines.mkString).as[OfferDetail]
  private val appConfigProps= readProperties()

  private def readProperties(): Map[String, String] = {
    import java.io.FileInputStream
    import java.util.Properties

    import scala.collection.JavaConverters._

    val prop = new Properties()
    prop.load(new FileInputStream(s"$userDir/test/resources/config/test-app-config.properties"))
    prop.asScala.toMap
  }

  val defaultTestAsyncAwaitTimeout = 1
  val defaultTestAsyncInterval= 500

  def getProductList = productList
  def getProductDetail = offerDetail
  def getProductDetailNoItems = offerDetailNoItems
  def getProductDetailNoItemsNoUpc = offerDetailNoItemsNoUpc
  def getProductDetailNoItemsParty2 = offerDetailNoItemsParty2
  def testConfigProperties = appConfigProps

  def getMockExecutionContext : RepositoryDispatcherContext = {
    val executionContext: ExecutionContext = scala.concurrent.ExecutionContext.global
    val repositoryDispatcher: RepositoryDispatcherContext = new MyMockedRepositoryDispatcher(executionContext)
    repositoryDispatcher
  }

  def getMockWorkerExecutionContext : WorkerDispatcherContext = {
    val executionContext: ExecutionContext = scala.concurrent.ExecutionContext.global
    val dispatcher: WorkerDispatcherContext = new MyMockedWorkerRepositoryDispatcher(executionContext)
    dispatcher
  }
}

class MyMockedRepositoryDispatcher(executionContext: ExecutionContext) extends RepositoryDispatcherContext(ActorSystem()) {
  override def execute(command: Runnable) = executionContext.execute(command)
  override def reportFailure(cause: Throwable) = executionContext.reportFailure(cause)
}

class MyMockedWorkerRepositoryDispatcher(executionContext: ExecutionContext) extends WorkerDispatcherContext(ActorSystem()) {
  override def execute(command: Runnable) = executionContext.execute(command)
  override def reportFailure(cause: Throwable) = executionContext.reportFailure(cause)
}
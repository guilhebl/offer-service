package common

import akka.actor.ActorSystem
import common.executor.{RepositoryDispatcherContext, WorkerDispatcherContext}
import play.api.libs.json.Json
import product.model.{OfferDetail, OfferList}

import scala.concurrent.ExecutionContext
import scala.io.Source

object MockBaseUtil {

  val MockFilesPath = "test/resources/mock/product"
  val MockMarketplaceFilesPath = s"$MockFilesPath/marketplace"

  /* MOCK data */
  private val userDir = System.getProperty("user.dir")
  private val productList = Json.parse(Source.fromFile(s"$MockFilesPath/sample_offer.json").getLines.mkString).as[OfferList]
  private val offerDetail = Json.parse(Source.fromFile(s"$MockFilesPath/sample_offer_detail.json").getLines.mkString).as[OfferDetail]
  private val offerDetailNoItems = Json.parse(Source.fromFile(s"$MockFilesPath/sample_offer_detail_no_detail_items.json").getLines.mkString).as[OfferDetail]
  private val offerDetailNoItemsNoUpc = Json.parse(Source.fromFile(s"$MockFilesPath/sample_offer_detail_no_upc_no_items.json").getLines.mkString).as[OfferDetail]
  private val offerDetailNoItemsParty2 = Json.parse(Source.fromFile(s"$MockFilesPath/sample_offer_detail_no_items_party2.json").getLines.mkString).as[OfferDetail]
  private val appConfigProps= readProperties()
  private val appConfigPropsRequestMonitorSpec = readPropertiesRequestMonitorSpec()

  private def readProperties(): Map[String, String] = {
    import java.io.FileInputStream
    import java.util.Properties

    import scala.collection.JavaConverters._

    val prop = new Properties()
    prop.load(new FileInputStream(s"$userDir/test/resources/config/test-app-config.properties"))
    prop.asScala.toMap
  }

  /**
    * Used for testing Request Monitor
    * @return overridden params
    */
  private def readPropertiesRequestMonitorSpec(): Map[String, String] = {
    val props = readProperties() +
      ("walmartUSMaxCapacity" -> "5") +
      ("walmartUSRequestMaxTries" -> "10") +
      ("walmartUSRequestWaitInterval" -> "200")
    props
  }

  val defaultTestAsyncAwaitTimeout = 1
  val defaultTestAsyncInterval= 500

  def getProductList = productList
  def getProductDetail = offerDetail
  def getProductDetailNoItems = offerDetailNoItems
  def getProductDetailNoItemsNoUpc = offerDetailNoItemsNoUpc
  def getProductDetailNoItemsParty2 = offerDetailNoItemsParty2
  def testConfigProperties = appConfigProps
  def testConfigPropertiesRequestMonitor: Map[String, String] = appConfigPropsRequestMonitorSpec

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

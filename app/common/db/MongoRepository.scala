package common.db

import common.config.AppConfigService
import common.db.Helpers._
import common.executor.WorkerDispatcherContext
import javax.inject.{Inject, Singleton}
import org.mongodb.scala.model.Filters._
import org.mongodb.scala.{Document, MongoCollection}
import play.api.{Logger, MarkerContext}

import scala.concurrent.Future

/**
  * A pure non-blocking interface
  */
trait MongoRepository {
  def search()(implicit mc: MarkerContext): Future[Option[Seq[Document]]]
  def get(id : String)(implicit mc: MarkerContext): Future[Option[Document]]
  def insert(r : Document)(implicit mc: MarkerContext): Future[Option[Document]]
  def insertMany(seq : Seq[Document])(implicit mc: MarkerContext): Future[Option[Seq[Document]]]
  def delete(id : String)(implicit mc: MarkerContext): Future[Option[Document]]
}

@Singleton
class MongoRepositoryImpl @Inject()
(appConfigService: AppConfigService, mongoService: MongoDbService)(implicit ec: WorkerDispatcherContext)
  extends MongoRepository {

  private lazy val logger = Logger(this.getClass)
  lazy val collectionName = appConfigService.properties("mongoDb.db.offer.collection.name")

  /**
    * Searches in DB
    * @return
    */
  override def search()(implicit mc: MarkerContext): Future[Option[Seq[Document]]] = {
    logger.trace(s"Mongo search")
    val db = mongoService.getDefaultDb
    val collection: MongoCollection[Document] = db.getCollection(collectionName)
    Future.successful(Some(collection.find().results()))
  }

  /**
    * Gets a record from Mongo DB
    *
    * @param id
    * @return
    */
  override def get(id : String)(implicit mc: MarkerContext): Future[Option[Document]] = {
    logger.trace(s"Db getDetails - : $id")
    val db = mongoService.getDefaultDb
    val collection: MongoCollection[Document] = db.getCollection(collectionName)
    val item = collection.find(equal("_id", id)).first().headResult()
    Future.successful(Some(item))
  }

  /**
    * Inserts a record in Mongo DB
    *
    * @return
    */
  override def insert(r : Document)(implicit mc: MarkerContext): Future[Option[Document]] = {
    logger.trace(s"Device Mongo insert - $r")
    val db = mongoService.getDefaultDb
    val collection: MongoCollection[Document] = db.getCollection(collectionName)
    collection.insertOne(r).results()
    Future.successful(Some(r))
  }

  /**
    * Inserts Many records in Jasper Mongo DB
    *
    * @return
    */
  override def insertMany(seq : Seq[Document])(implicit mc: MarkerContext): Future[Option[Seq[Document]]] = {
    logger.trace(s"Device Mongo insertMany - $seq")
    val db = mongoService.getDefaultDb
    val collection: MongoCollection[Document] = db.getCollection(collectionName)
    collection.insertMany(seq).results()
    Future.successful(Some(seq))
  }

  /**
    * Delete a record in Jasper Mongo DB
    *
    * @return
    */
  override def delete(id : String)(implicit mc: MarkerContext): Future[Option[Document]] = {
    logger.trace(s"Device Mongo delete - $id")
    val db = mongoService.getDefaultDb
    val collection: MongoCollection[Document] = db.getCollection(collectionName)
    val item = collection.find(equal("_id", id)).first().headResult()
    collection.deleteOne(equal("_id", id)).results()
    Future.successful(Some(item))
  }

}

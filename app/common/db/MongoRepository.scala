package common.db

import common.config.AppConfigService
import common.db.Helpers._
import common.executor.RepositoryDispatcherContext
import javax.inject.{Inject, Singleton}
import org.mongodb.scala.model.Filters._
import org.mongodb.scala.model.Projections._
import org.mongodb.scala.{Document, MongoCollection}
import play.api.{Logger, MarkerContext}

import scala.concurrent.Future

/**
  * A pure non-blocking interface
  */
trait MongoRepository {
  def search(collectionName: String, fields: Option[Seq[String]] = None)(implicit mc: MarkerContext): Future[Seq[Document]]
  def getAllInStringField(collectionName: String, inField: String, inSeq: Seq[String], fields: Option[Seq[String]] = None)
                         (implicit mc: MarkerContext): Future[Seq[Document]]
  def get(collectionName: String, id : String)(implicit mc: MarkerContext): Future[Option[Document]]
  def getCollection(collectionName: String)(implicit mc: MarkerContext): MongoCollection[Document]
  def insert(collectionName: String, r : Document)(implicit mc: MarkerContext): Future[Option[Document]]
  def insertMany(collectionName: String, seq : Seq[Document])(implicit mc: MarkerContext): Future[Option[Seq[Document]]]
  def delete(collectionName: String, id : String)(implicit mc: MarkerContext): Future[Option[Document]]
}

/**
  * MongoDB driver supports non-blocking async operations, as much as possible use the async way of calling Mongo
  * to achieve better performance
  *
  * @param appConfigService
  * @param mongoService
  * @param ec
  */
@Singleton
class MongoRepositoryImpl @Inject()
(appConfigService: AppConfigService, mongoService: MongoDbService)(implicit ec: RepositoryDispatcherContext)
  extends MongoRepository {

  private lazy val logger = Logger(this.getClass)
  private lazy val db = mongoService.getDefaultDb


  override def getCollection(collectionName: String)(implicit mc: MarkerContext): MongoCollection[Document] = {
    db.getCollection(collectionName)
  }

  /**
    * Searches documents in DB
    *
    * @param collectionName
    * @param fields - specify fields to select
    *
    * @return
    */
  override def search(collectionName: String, fields: Option[Seq[String]] = None)(implicit mc: MarkerContext): Future[Seq[Document]] = {
    logger.trace(s"Mongo search")
    val collection: MongoCollection[Document] = getCollection(collectionName)

    fields match {
      case Some(x) => collection.find().projection(include(fields.get : _*)).toFuture()
      case _ => collection.find().toFuture()
    }
  }

  /**
    * Searches documents in DB all in collection of strings
    *
    * @param collectionName
    * @param fields - specify fields to select
    *
    * @return
    */
  override def getAllInStringField(collectionName: String, inField: String, inSeq: Seq[String], fields: Option[Seq[String]] = None)(implicit mc: MarkerContext): Future[Seq[Document]] = {
    logger.trace(s"Mongo search In")
    val collection: MongoCollection[Document] = getCollection(collectionName)

    fields match {
      case Some(x) => collection.find().projection(in(inField, inSeq)).projection(include(fields.get : _*)).toFuture()
      case _ => collection.find().projection(in(inField, inSeq)).toFuture()
    }
  }

  /**
    * Gets a document from Mongo DB
    *
    * @param id
    * @return
    */
  override def get(collectionName: String, id : String)(implicit mc: MarkerContext): Future[Option[Document]] = {
    logger.trace(s"Db getDetails - : $id")

    val collection: MongoCollection[Document] = getCollection(collectionName)
    val item = collection.find(equal("_id", id)).first()
    item.toFutureOption()
  }

  /**
    * Inserts a document in Mongo DB
    *
    * @return
    */
  override def insert(collectionName: String, r : Document)(implicit mc: MarkerContext): Future[Option[Document]] = {
    logger.trace(s"Device Mongo insert - $r")
    val collection: MongoCollection[Document] = getCollection(collectionName)

    collection.insertOne(r).toFutureOption().map {
      case Some(x) => Some(r)
      case _ => None
    }
  }

  /**
    * Inserts Many documents in Mongo DB
    *
    * @return
    */
  override def insertMany(collectionName: String, seq: Seq[Document])(implicit mc: MarkerContext): Future[Option[Seq[Document]]] = {
    logger.trace(s"Device Mongo insertMany - $seq")
    val collection: MongoCollection[Document] = getCollection(collectionName)

    collection.insertMany(seq).toFutureOption().map {
      case Some(x) => Some(seq)
      case _ => None
    }
  }

  /**
    * Delete a document in Mongo DB
    *
    * @return
    */
  override def delete(collectionName: String, id: String)(implicit mc: MarkerContext): Future[Option[Document]] = {
    logger.trace(s"Device Mongo delete - $id")
    val collection: MongoCollection[Document] = getCollection(collectionName)
    val item = collection.find(equal("_id", id)).first().headResult()
    if (item.isEmpty) return Future.successful(None)

    collection.deleteOne(equal("_id", id)).toFutureOption().map {
      case Some(x) => Some(item)
      case _ => None
    }
  }

}

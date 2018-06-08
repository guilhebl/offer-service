package common.db

import common.config.AppConfigService
import javax.inject._
import org.mongodb.scala._

trait MongoDbService {
  def getClient: MongoClient
  def getDefaultDb: MongoDatabase
}

@Singleton
class MongoDbServiceImpl @Inject()(appConfigService: AppConfigService) extends MongoDbService {
  private lazy val mongoClient: MongoClient = MongoClient(appConfigService.properties("mongoDb.url"))
  private lazy val mongoDefaultDb: MongoDatabase = mongoClient.getDatabase(appConfigService.properties("mongoDb.db"))

  override def getClient: MongoClient = mongoClient
  override def getDefaultDb: MongoDatabase = mongoDefaultDb
}

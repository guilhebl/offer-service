package common.db

import org.mongodb.scala.bson.ObjectId

trait MongoEntity {
  val _id: ObjectId
}

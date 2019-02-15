package common.db

import org.bson.codecs.configuration.CodecRegistries.{fromProviders, fromRegistries}
import org.mongodb.scala.bson.codecs.DEFAULT_CODEC_REGISTRY
import org.mongodb.scala.bson.codecs.Macros._
import product.model.{Offer, OfferPriceLog, ProductTracking}

object CodecProviders {
  lazy val codecRegistry = fromRegistries( fromProviders(
    classOf[Offer],
    classOf[OfferPriceLog],
    classOf[ProductTracking]
  ), DEFAULT_CODEC_REGISTRY )
}

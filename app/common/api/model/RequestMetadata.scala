package common.api.model

import common.encrypt.model.EncryptionPair
import play.api.libs.json.Json

/**
  * Encapsulates common metadata request info
  *
  * @param header
  */
case class RequestMetadata(
  header: EncryptionPair
)

object RequestMetadata {

  /**
    * Mapping to and from JSON.
    */
  implicit val formatter = Json.format[RequestMetadata]
}

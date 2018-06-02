package common.encrypt.model

import play.api.libs.json.Json

/**
  * Encapsulates an encrypted value and a nonce
  *
  * @param encrypted
  * @param nonce
  */
case class EncryptionPair(encrypted: String, nonce: String)

object EncryptionPair {
  implicit val documentFormatter = Json.format[EncryptionPair]
}

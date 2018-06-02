package common.encrypt

import java.security.SecureRandom
import java.util.Base64

import common.encrypt.model.EncryptionPair
import javax.crypto.Cipher
import javax.crypto.spec.{GCMParameterSpec, SecretKeySpec}
import play.api.Logger

object EncryptUtil {

  // AES-GCM parameters
  val GCM_NONCE_LENGTH = 12 // in bytes
  val GCM_TAG_LENGTH = 16

  private val logger = Logger(getClass)

  /**
    * Generate a new encryption key.
    */
  private def generateKey(key: String) = new SecretKeySpec(key.getBytes, "AES")

  def encryptAesGcm(key : String, s:String): Option[EncryptionPair] = {
    try {
      // Initialise random and generate key
      val input = s.getBytes

      // Encrypt
      val k = generateKey(key)
      val cipher = Cipher.getInstance("AES/GCM/NoPadding")
      val nonce = new Array[Byte](GCM_NONCE_LENGTH)
      val random = SecureRandom.getInstanceStrong
      random.nextBytes(nonce)
      val spec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, nonce)
      cipher.init(Cipher.ENCRYPT_MODE, k, spec)
      val cipherText = cipher.doFinal(input)
      Some(EncryptionPair(Base64.getEncoder.encodeToString(cipherText), Base64.getEncoder.encodeToString(nonce)))

    } catch {
      case e: Exception =>
        logger.trace("error while encrypting using AES" + e.getMessage)
        None
      case _: Exception =>
        logger.trace("error while encrypting using AES GCM")
        None
    }
  }

  def decryptAesGcm(key: String, encrypted: String, nonce: String): Option[String] = {
    try {
      val bytes = Base64.getDecoder.decode(encrypted)
      val bytesNonce = Base64.getDecoder.decode(nonce)
      val k = generateKey(key)
      val cipher = Cipher.getInstance("AES/GCM/NoPadding")
      val spec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, bytesNonce)
      cipher.init(Cipher.DECRYPT_MODE, k, spec)

      val decValue = cipher.doFinal(bytes)
      val plaintext = new String(decValue)
      Some(plaintext)
    } catch {
      case e: Exception =>
        logger.trace("error while decrypting AES Gcm" + e.getMessage)
        None
      case _: Exception =>
        logger.trace("error while decrypting AES Gcm")
        None
    }
  }

}

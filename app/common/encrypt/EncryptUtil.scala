package common.encrypt

import java.security.{MessageDigest, SecureRandom}
import java.util.Base64

import common.encrypt.model.EncryptionPair
import javax.crypto.Cipher
import javax.crypto.spec.{GCMParameterSpec, IvParameterSpec, SecretKeySpec}
import org.apache.commons.codec.binary.Hex
import play.api.Logger

import scala.util.Random

object EncryptUtil {

  private val AES = "AES"

  // AES-GCM parameters
  val GCM_NONCE_LENGTH = 12 // in bytes
  val GCM_TAG_LENGTH = 16

  private lazy val logger = Logger(getClass)

  /**
    * Generate a new encryption key.
    */
  private def generateKey(key: String) = new SecretKeySpec(key.getBytes, "AES")

  def encryptAES(key : String, s:String): String = {
    try {
      val aesKey = new SecretKeySpec(key.getBytes(), AES)
      val cipher = Cipher.getInstance(AES)

      cipher.init(Cipher.ENCRYPT_MODE, aesKey)
      val encrypted = cipher.doFinal(s.getBytes())
      val r = new String(Base64.getEncoder.encode(encrypted), "UTF-8")
      r
    } catch {
      case e: Exception =>
        logger.trace("error while encrypting using AES Simple" + e.getMessage)
        ""
      case _: Exception =>
        logger.trace("error while encrypting using AES Simple")
        ""
    }
  }

  def decryptAES(key : String, s:String): String = {
    try {
      val aesKey = new SecretKeySpec(key.getBytes(), AES)
      val cipher = Cipher.getInstance(AES)

      cipher.init(Cipher.DECRYPT_MODE, aesKey)
      val decrypted = new String(cipher.doFinal(Base64.getDecoder.decode(s)), "UTF-8")
      decrypted
    } catch {
      case e: Exception =>
        logger.trace("error while decrypting using AES Simple" + e.getMessage)
        ""
      case _: Exception =>
        logger.trace("error while decrypting using AES Simple")
        ""
    }
  }

  def encryptAesWithNonce(key : String, s:String): Option[EncryptionPair] = {
    try {
      val nonce: String = RandomStringGenerator.generate(12)
      val encrypted = encryptAES(key, nonce + s)
      Some(EncryptionPair(encrypted, nonce))
    }
    catch {
      case e: Exception =>
        logger.trace("error while encrypting using AES with nonce" + e.getMessage)
        None
      case _: Exception =>
        logger.trace("error while encrypting using AES with nonce")
        None
    }
  }

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

  val InstancePKCS5Padding = "AES/CBC/PKCS5Padding"
  val InstanceNoPadding = "AES/CBC/NoPadding"

  def encrypt(decrypted: String, password: String, instance: String = InstancePKCS5Padding): String = {

    val SHA256 = MessageDigest.getInstance("SHA-256")
    SHA256.update((password).getBytes())
    val key = SHA256.digest()
    val keyspec = new SecretKeySpec(key, getAlgorithm(instance))

    var iv = new Array[Byte](16)
    Random.nextBytes(iv)
    val ivspec = new IvParameterSpec(iv)
    val ivBase64 = java.util.Base64.getEncoder.encode(iv).filterNot("=".toSet)

    val cipher = Cipher.getInstance(instance)
    cipher.init(Cipher.ENCRYPT_MODE, keyspec, ivspec)
    val utf8 = decrypted.getBytes("UTF-8")
    val MD5 = MessageDigest.getInstance("MD5")
    MD5.update(utf8)
    val encrypted = cipher.doFinal(
      pkcs5Pad(utf8 ++ new String(Hex.encodeHex(MD5.digest())).getBytes("UTF-8"))
    )

    val encBase64 = Base64.getEncoder.encode(encrypted)
    new String(ivBase64 ++ encBase64, "UTF-8")
  }

  def decrypt(encrypted: String, password: String, instance: String = InstancePKCS5Padding): String = {

    val messageDigest = MessageDigest.getInstance("SHA-256")
    messageDigest.update(password.getBytes())
    val key = messageDigest.digest()
    val keyspec = new SecretKeySpec(key, getAlgorithm(instance))

    val iv = java.util.Base64.getDecoder.decode(encrypted.take(22) + "==")
    val ivspec = new IvParameterSpec(iv)

    val decoded = java.util.Base64.getDecoder.decode(encrypted.substring(22, encrypted.length()))
    val cipher = Cipher.getInstance(instance)
    cipher.init(Cipher.DECRYPT_MODE, keyspec, ivspec)
    val dec = cipher.doFinal(decoded)
    val decrypted = pkcs5Unpad(dec)

    val message = decrypted.take(decrypted.length - 32)
    val md5 = decrypted.takeRight(32)

    val MD5 = MessageDigest.getInstance("MD5")
    MD5.update(message)
    val messageHex = new String(Hex.encodeHex(MD5.digest()))
    if (messageHex != new String(md5, "UTF-8")) {
      throw new Exception("[error][" + this.getClass().getName() + "] " +
        "Message could not be decrypted correctly.\n" +
        "\tMessage: \"" + new String(message, "UTF-8") + "\"\n" +
        "The provided hashes are not equal:\n" +
        "\tGenerated hash: " + messageHex + "\n" +
        "\tExpected  hash: " + md5 + "\n"
      )
    }
    new String(message, "UTF-8")
  }

  def pkcs5Pad(input: Array[Byte], size: Int = 16): Array[Byte] = {
    val padByte: Int = size - (input.length % size)
    return input ++ Array.fill[Byte](padByte)(padByte.toByte)
  }

  def pkcs5Unpad(input: Array[Byte]): Array[Byte] = {
    val padByte = input.last
    val length = input.length
    if (padByte > length) throw new Exception("The input was shorter than the padding byte indicates")
    if (!input.takeRight(padByte).containsSlice(Array.fill[Byte](padByte)(padByte))) throw new Exception("Padding format is not as being expected")
    input.take(length - padByte)
  }

  private def getAlgorithm(s: String): String = """(.*?)\/""".r.findFirstMatchIn(s).get.group(1)

}

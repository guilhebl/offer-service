package common.encrypt

import java.security.SecureRandom
import java.util.UUID.randomUUID

/**
  * Generates random alpha-numeric strings
  */
object RandomStringGenerator {
  private val SYMBOLS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
  private val random = new SecureRandom

  def generate(length: Int): String = {
    val buf = new Array[Char](length)
    var idx = 0
    while (idx < length) {
      buf(idx) = SYMBOLS.charAt(random.nextInt(SYMBOLS.length))
      idx += 1
    }
    new String(buf)
  }

  def generateUUIDString(): String = {
    randomUUID().toString
  }

}

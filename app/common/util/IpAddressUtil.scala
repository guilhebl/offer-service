package common.util

import java.net.InetAddress

/**
  * Provides utility methods for ip mapping
  */
object IpAddressUtil {

  def ipv4ToLong(ip: String): Option[Long] = {
    val addrArray: Array[String] = ip.split("\\.")
    if (addrArray.length != 4) {
      None
    } else {
      var num: Long = 0
      var i: Int = 0
      while (i < addrArray.length) {
        val power: Int = 3 - i
        val n = addrArray(i).toInt
        if (n < 0 || n > 255) {
          None
        } else {
          num = num + ((n % 256) * Math.pow(256, power)).toLong
          i += 1
        }
      }
      Some(num)
    }
  }

  def longToIPv4 (ip : Long) : String = {
    val bytes: Array[Byte] = new Array[Byte](4)
    bytes(0) = ((ip & 0xff000000) >> 24).toByte
    bytes(1) = ((ip & 0x00ff0000) >> 16).toByte
    bytes(2) = ((ip & 0x0000ff00) >> 8).toByte
    bytes(3) = (ip & 0x000000ff).toByte
    InetAddress.getByAddress(bytes).getHostAddress()
  }
}

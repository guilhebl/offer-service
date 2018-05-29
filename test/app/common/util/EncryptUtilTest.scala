package app.common.util

import common.UnitSpec
import common.util.EncryptUtil

import scala.util.Random

object EncryptUtilTestHelper {
  def randomPrintableChars(length: Int): Array[Char] = Array.fill(length)(Random.nextPrintableChar)
  def randomPrintableString(length: Int): String = new String(randomPrintableChars(length))
}

class EncryptUtilTest extends UnitSpec {

  "Usage Example" should "work" in {

    // We start with a strong password
    val password = "a strong passphrase is important for symmetric encryption"

    // And of course you have something to encrypt
    val data = "Lorem ipsum dolor sit amet"

    // Now we have all we need
    val encrypted = EncryptUtil.encrypt(data, password)
    val decrypted = EncryptUtil.decrypt(encrypted, password)

    assert(data == decrypted)
  }

  "AES/CBC/NoPadding ASCII encryption" should "work" in {
    val password = "myStrongPassword1234567890"
    val str = "Hello World! This is a very long text used for testing purposes only, please expect this text to be encrypted"

    val enc = EncryptUtil.encrypt(str, password, EncryptUtil.InstanceNoPadding)
    val dec = EncryptUtil.decrypt(enc, password, EncryptUtil.InstanceNoPadding)

    assert(str == dec)
  }

  "AES/CBC/NoPadding UTF-8 encryption" should "work" in {
    val password = "myStrongPassword1234567890"
    val str = "Hælló World Ȱ ƛ Ƶ Ʒ Ɇ 1234567890"
    
    
    val enc = EncryptUtil.encrypt(str, password, EncryptUtil.InstanceNoPadding)
    val dec = EncryptUtil.decrypt(enc, password, EncryptUtil.InstanceNoPadding)

    assert(str == dec)
  }

  "AES/CBC/NoPadding random String encryption" should "work" in {
    val password = "myStrongPassword1234567890"
    val size = Random.nextInt(20)
    val str = EncryptUtilTestHelper.randomPrintableString(size)

    val enc = EncryptUtil.encrypt(str, password, EncryptUtil.InstanceNoPadding)
    val dec = EncryptUtil.decrypt(enc, password, EncryptUtil.InstanceNoPadding)

    assert(str == dec)
  }

  "AES/CBC/PKCS5Padding ASCII encryption" should "work" in {
    val password = "myStrongPassword1234567890"
    val str = "Hello World! This is a very long text used for testing purposes only, please expect this text to be encrypted"

    val enc = EncryptUtil.encrypt(str, password, EncryptUtil.InstancePKCS5Padding)
    val dec = EncryptUtil.decrypt(enc, password, EncryptUtil.InstancePKCS5Padding)

    assert(str == dec)
  }

  "AES/CBC/PKCS5Padding UTF-8 encryption" should "work" in {
    val password = "myStrongPassword1234567890"
    val str = "Hælló World Ȱ ƛ Ƶ Ʒ Ɇ 1234567890"

    val enc = EncryptUtil.encrypt(str, password, EncryptUtil.InstancePKCS5Padding)
    val dec = EncryptUtil.decrypt(enc, password, EncryptUtil.InstancePKCS5Padding)

    assert(str == dec)
  }

  "AES/CBC/PKCS5Padding random String encryption" should "work" in {
    val password = "myStrongPassword1234567890"
    val size = Random.nextInt(20)
    val str = EncryptUtilTestHelper.randomPrintableString(size)

    val enc = EncryptUtil.encrypt(str, password, EncryptUtil.InstancePKCS5Padding)
    val dec = EncryptUtil.decrypt(enc, password, EncryptUtil.InstancePKCS5Padding)

    assert(str == dec)
  }

  "padding ASCII" should "work" in {
    val str = "Hello World! This is a very long text used for testing purposes only, please expect this text to be encrypted"

    val padded = EncryptUtil.pkcs5Pad(str.getBytes("UTF-8"))
    val unpadded = EncryptUtil.pkcs5Unpad(padded)

    assert(str == new String(unpadded, "UTF-8"))
  }

  "padding UTF-8" should "work" in {
    val str = "Hælló World Ȱ ƛ Ƶ Ʒ Ɇ 1234567890"

    val padded = EncryptUtil.pkcs5Pad(str.getBytes("UTF-8"))
    val unpadded = EncryptUtil.pkcs5Unpad(padded)

    assert(str == new String(unpadded, "UTF-8"))
  }

  "padding random String" should "work" in {
    val size = Random.nextInt(20)
    val str = EncryptUtilTestHelper.randomPrintableString(size)

    val padded = EncryptUtil.pkcs5Pad(str.getBytes("UTF-8"))
    val unpadded = EncryptUtil.pkcs5Unpad(padded)

    assert(str == new String(unpadded, "UTF-8"))
  }

}
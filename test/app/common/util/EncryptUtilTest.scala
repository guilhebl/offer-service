package app.common.util

import common.UnitSpec
import common.encrypt.EncryptUtil

class EncryptUtilTest extends UnitSpec {

  "AES encryption" should "work" in {
    val key = "q4t6w9z$C&F)J@Ncq4t6w9z$C&F)J@N1"
    val plaintext = "True knowledge exists in knowing that you know nothing."
    val enc = EncryptUtil.encryptAES(key, plaintext)
    val decrypted = EncryptUtil.decryptAES(key, enc)
    assert(plaintext == decrypted)
  }

  "AES with nonce encryption" should "work" in {
    val key = "z5t6w9z$C&F)J@Ncq4t6w9z$C&F)J@N1"
    val plaintext = "By the skin of my teeth."
    val encPair = EncryptUtil.encryptAesWithNonce(key, plaintext)
    val decrypted = EncryptUtil.decryptAES(key, encPair.get.encrypted)
    assert(encPair.get.nonce + plaintext == decrypted)
  }

}
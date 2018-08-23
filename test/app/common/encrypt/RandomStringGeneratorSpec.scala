package app.common.encrypt

import common.UnitSpec
import common.encrypt.RandomStringGenerator

class RandomStringGeneratorSpec extends UnitSpec {

  "Usage Example" should "work" in {
    val string1 = RandomStringGenerator.generate(16)
    assert(string1 != "" && string1.length == 16)

    val string2 = RandomStringGenerator.generate(32)
    assert(string2 != "" && string2.length == 32)
  }

}
package common

import mockws.MockWSHelpers
import org.scalatest.BeforeAndAfterAll
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec

abstract class BaseDomainTest extends PlaySpec with MockitoSugar with MockWSHelpers with BeforeAndAfterAll {

  override def afterAll(): Unit = {
    shutdownHelpers()
  }

}

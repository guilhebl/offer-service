package common
import mockws.MockWSHelpers
import org.scalatest.mockito.MockitoSugar
import org.scalatest.prop.PropertyChecks
import org.scalatest.{BeforeAndAfterAll, FunSuite, Matchers}

abstract class BaseFunSuiteDomainTest extends FunSuite with Matchers with PropertyChecks with MockitoSugar with MockWSHelpers with BeforeAndAfterAll {

  override def afterAll(): Unit = {
    shutdownHelpers()
  }

}

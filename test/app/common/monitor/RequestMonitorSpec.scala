package app.common.monitor

import java.time.ZonedDateTime

import common.MockBaseUtil._
import common.UnitSpec
import common.config.AppConfigService
import common.monitor.{RequestMonitor, RequestMonitorFixedCycleImpl, RequestMonitorRateLimiterImpl}
import org.junit.Assert
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import product.marketplace.common.MarketplaceConstants._

class RequestMonitorSpec extends UnitSpec with MockitoSugar {

  val appConfigMock = mock[AppConfigService]
  when(appConfigMock.properties) thenReturn testConfigPropertiesRequestMonitor

  "RateLimiter monitor" should "be valid" in {
    val monitor: RequestMonitor = new RequestMonitorRateLimiterImpl(appConfigMock)

    val startTime = ZonedDateTime.now.getSecond
    var i = 0
    while( i < 5 ) {
      Assert.assertTrue("monitor not acquired", monitor.isRequestPossible(Walmart))
      i = i + 1
    }
    val elapsedTimeSeconds = ZonedDateTime.now.getSecond - startTime
    Assert.assertTrue("Unable to acquire 5 permits within a second", elapsedTimeSeconds <= 1)
  }

  "FixedCycle monitor" should "be valid" in {
    val monitor: RequestMonitor = new RequestMonitorFixedCycleImpl(appConfigMock)

    val startTime = ZonedDateTime.now.getSecond
    var i = 0
    while( i < 5 ) {
      Assert.assertTrue("monitor not acquired", monitor.isRequestPossible(Walmart))
      i = i + 1
    }
    val elapsedTimeSeconds = ZonedDateTime.now.getSecond - startTime
    Assert.assertTrue("Unable to acquire 5 permits within a second", elapsedTimeSeconds <= 1)
  }

}
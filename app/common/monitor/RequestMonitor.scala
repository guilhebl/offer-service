package common.monitor

import common.config.AppConfigService
import javax.inject.{Inject, Singleton}

/**
* Defines a request monitor, for a given key or entity provides the logic to detect if this entity is able
  * to perform a certain action. Limits actions by constraints, for example an API backend that might handle
  * at max 5 calls per second, the implementation classes will ensure that the limits are observed.
  */
trait RequestMonitor {
  def isRequestPossible(name: String): Boolean
}

@Singleton
class RequestMonitorImpl @Inject()(appConfigService: AppConfigService, rateLimiterImpl: RequestMonitorRateLimiterImpl, fixedCycleImpl: RequestMonitorFixedCycleImpl)
  extends RequestMonitor {

  val monitorRateLimiterImplTypeEnabled: Boolean = appConfigService.properties("requestMonitorRateLimiterImplEnabled").toBoolean

  override def isRequestPossible(userName: String): Boolean = {
    if (monitorRateLimiterImplTypeEnabled) {
      rateLimiterImpl.isRequestPossible(userName)
    } else {
      fixedCycleImpl.isRequestPossible(userName)
    }
  }
}

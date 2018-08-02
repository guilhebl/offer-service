package common.scheduler

import akka.actor.ActorSystem
import common.config.AppConfigService
import common.email.EmailService
import common.email.model.EmailRequest
import common.executor.RepositoryDispatcherContext
import javax.inject.{Inject, Singleton}
import play.api.Logger
import play.api.inject.ApplicationLifecycle

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

@Singleton
class EmailTask @Inject()(
  emailService: EmailService,
  appConfigService: AppConfigService,
  actorSystem: ActorSystem,
  lifecycle: ApplicationLifecycle
)(
  implicit executionContext: RepositoryDispatcherContext
) {

  private lazy val logger = Logger(this.getClass)
  private lazy val intervalSeconds = appConfigService.properties("scheduler.job.frequency.seconds").toInt

  actorSystem.scheduler.schedule(initialDelay = intervalSeconds.seconds, interval = intervalSeconds.second) {

    val isEnabled = appConfigService.properties("scheduler.job.email.enabled").toBoolean
    logger.info(s"Email Job starting... job enabled: $isEnabled")

    if (isEnabled) {
      val from = appConfigService.properties("email.default.from")
      val to = appConfigService.properties("email.default.to").split(",")
      val content = views.html.emailtest("TEST", "12345")

      val response = emailService.sendEmail(
        EmailRequest(
          "test",
          from,
          to,
          Some(content.body)
        )
      )
      logger.info(s"Email sent: $response")
    }
    logger.info(s"Email Job completed")
  }

  // This is necessary to avoid thread leaks, specially if you are using a custom ExecutionContext
  lifecycle.addStopHook { () =>
    Future.successful(
      Await.result(actorSystem.terminate(), 60.seconds)
    )
  }

}

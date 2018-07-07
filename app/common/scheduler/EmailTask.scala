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

  actorSystem.scheduler.schedule(initialDelay = 0.seconds, interval = intervalSeconds.second) {
    logger.info("Email Job starting...")

    val response = emailService.sendEmail(
      EmailRequest(
        "test",
        "searchprodmail@gmail.com",
        Seq("searchprodmailtest1@gmail.com"),
        "test",
        None
      )
    )

    logger.info(s"Email Job completed: $response")
  }

  // This is necessary to avoid thread leaks, specially if you are using a custom ExecutionContext
  lifecycle.addStopHook { () =>
    Future.successful(
      Await.result(actorSystem.terminate(), 60.seconds)
    )
  }

}

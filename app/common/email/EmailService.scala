package common.email

import common.config.AppConfigService
import common.email.model.{EmailRequest, EmailResponse}
import common.executor.WorkerDispatcherContext
import javax.inject.{Inject, Singleton}
import play.api.Logger
import play.api.libs.mailer._

/**
  * A pure non-blocking interface for the EmailService.
  */
trait EmailService {
  def sendEmail(emailRequest: EmailRequest): EmailResponse
}

/**
  * An implementation for the email service
  *
  */
@Singleton
class EmailServiceImpl @Inject()(mailerClient: MailerClient, appConfigService: AppConfigService)(implicit ec: WorkerDispatcherContext)
    extends EmailService {

  private val logger = Logger(this.getClass)
  private val isEnabled = appConfigService.properties("email.enabled").toBoolean

  override def sendEmail(emailRequest: EmailRequest): EmailResponse = {
    logger.info(s"sending email: $emailRequest")
    if (isEnabled) {
      val email = Email(
        emailRequest.subject,
        emailRequest.from,
        emailRequest.to,
        bodyHtml = emailRequest.htmlBodyContent
      )
      val result = mailerClient.send(email)
      EmailResponse.ok(result)

    } else {
      logger.info("preparing to send email - email disabled")
      EmailResponse.ok("Email service disabled")
    }
  }

}

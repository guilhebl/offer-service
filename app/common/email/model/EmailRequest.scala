package common.email.model

import play.api.libs.json.Json

case class EmailRequest(
  subject: String,
  from: String,
  to: Seq[String],
  htmlBodyContent: Option[String] = None
)

object EmailRequest {
  implicit val formatter = Json.format[EmailRequest]
}

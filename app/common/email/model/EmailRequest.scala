package common.email.model

import play.api.libs.json.Json
import product.model.NameValue

case class EmailRequest(
  subject: String,
  from: String,
  to: Seq[String],
  templateId: String,
  props: Option[Seq[NameValue]]
)

object EmailRequest {
  implicit val formatter = Json.format[EmailRequest]
}

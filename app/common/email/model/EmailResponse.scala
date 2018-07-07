package common.email.model

import common.api.constants.MessageConstants
import common.api.model.ResponseMetadata
import play.api.libs.json.{Json, OFormat}

/**
  * response for sending an email
  *
  */
case class EmailResponse(
  metadata: ResponseMetadata
)

object EmailResponse {
  implicit val documentFormatter: OFormat[EmailResponse] = Json.format[EmailResponse]

  def ok(msg: String): EmailResponse = {
    EmailResponse(ResponseMetadata(MessageConstants.OK, msg))
  }
}

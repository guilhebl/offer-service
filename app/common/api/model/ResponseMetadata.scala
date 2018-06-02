package common.api.model

import common.api.constants.MessageConstants._
import play.api.libs.json.Json

/**
  * Encapsulates common metadata response info
  *
  * @param code - response code
  * @param message - response message
  */
case class ResponseMetadata(code: String, message: String)

object ResponseMetadata {
  implicit val formatter = Json.format[ResponseMetadata]

  def empty(): ResponseMetadata = {
    new ResponseMetadata(OK, OK_MSG)
  }
}

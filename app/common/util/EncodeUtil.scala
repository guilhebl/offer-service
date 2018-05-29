package common.util

import com.google.common.base.Charsets
import com.google.common.io.BaseEncoding

object EncodeUtil {
  def encodeUserKey(user : String, key : String) : String = {
    BaseEncoding.base64().encode(s"$user:$key".getBytes(Charsets.UTF_8))
  }
}

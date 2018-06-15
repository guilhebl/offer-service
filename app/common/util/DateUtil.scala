package common.util

import java.text.SimpleDateFormat
import java.util.{Calendar, Date, TimeZone}


object DateUtil {

  private val df : SimpleDateFormat = buildSimpleDateFormat

  private def buildSimpleDateFormat: SimpleDateFormat = {
    val formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
    val tz = TimeZone.getTimeZone("UTC")
    formatter.setTimeZone(tz)
    formatter
  }

  /**
    * Gets formatted date according to pattern above if date is empty returns formatted date now
    * @param date
    * @return
    */
  def format(date : Option[Date]) : String = {
    date match {
      case Some(x) => df.format(x.getTime)
      case _ => df.format(Calendar.getInstance.getTime)
    }
  }

}

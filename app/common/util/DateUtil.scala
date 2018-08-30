package common.util

import java.text.SimpleDateFormat
import java.time.{LocalDateTime, LocalTime, ZoneId}
import java.util.{Calendar, Date, TimeZone}

import org.joda.time.{DateTime, DateTimeZone}

object DateUtil {

  private val df : SimpleDateFormat = buildSimpleDateFormat

  private def buildSimpleDateFormat: SimpleDateFormat = {
    val formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
    val tz = TimeZone.getDefault
    formatter.setTimeZone(tz)
    formatter
  }

  /**
    * Gets formatted date according to pattern above if date is empty returns formatted date now
    * @param date
    * @return
    */
  def format(date: Date) : String = {
    df.format(date.getTime)
  }

  /**
    * Gets formatted date according to pattern above if date is empty returns formatted date now
    * @param date
    * @return
    */
  def format(date : Option[Date]) : String = {
    date match {
      case Some(x) => format(x)
      case _ => df.format(Calendar.getInstance.getTime)
    }
  }

  /**
    * Gets formatted date according to pattern
    * @param time timestamp
    * @return
    */
  def format(time: Long) : String = {
    format(new Date(time))
  }

  /**
    * Gets date from string
    * @param date
    * @return
    */
  def parse(date : String): Option[Date] = {
    if (date == "") None
    else Some(df.parse(date))
  }

  /**
    *
    * @param testDate
    * @param startDate
    * @param endDate
    * @return
    */
  def isWithinRange(testDate: Date, startDate: Date, endDate: Date): Boolean = {
    testDate.after(startDate) && testDate.before(endDate)
  }

  def atStartOfDay(date: Date): Date = {
    val localDateTime = dateToLocalDateTime(date)
    val startOfDay = localDateTime.`with`(LocalTime.MIN)
    localDateTimeToDate(startOfDay)
  }

  def atEndOfDay(date: Date): Date = {
    val localDateTime = dateToLocalDateTime(date)
    val endOfDay = localDateTime.`with`(LocalTime.MAX)
    localDateTimeToDate(endOfDay)
  }

  /**
    * gets the timestamp for end of month and last time of day for the Nth last month
    */
  def getLastNthEndOfMonthTimestamp(n: Int): Date = {
    val cal = Calendar.getInstance()
    cal.add(Calendar.MONTH, -n)
    cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
    val date = DateUtil.atEndOfDay(cal.getTime)
    date
  }


  /**
    * gets last N months and the date timestamp of the last day of the month at the last time
    * of day (in system default timezone)
    */
  def getLastMonthsTimestampEnd(n: Int): Seq[Date] = {
    val dates = (1 to n).map(getLastNthEndOfMonthTimestamp)
    dates
  }

  /**
    * Checks if a given date is in current month and year
    */
  def isWithinMonthAndYear(d: Date): Boolean = {
    val cal = Calendar.getInstance
    isWithinMonthAndYear(d, cal.getTime)
  }

  /**
    * Checks if a given date is in same month and year
    */
  def isWithinMonthAndYear(d: Date, d2: Date): Boolean = {
    val zone = DateTimeZone.getDefault()
    val dateTime = new DateTime(d, zone)
    val dateTime2 = new DateTime(d2, zone)

    // Now see if the month and year match.
    dateTime.getMonthOfYear == dateTime2.getMonthOfYear && dateTime.getYear == dateTime2.getYear
  }

  /**
    * get yesterday
    */
  def getYesterday(d: Date): Date = {
    val cal = Calendar.getInstance()
    cal.add(Calendar.SECOND, -86400)
    cal.getTime
  }

  private def dateToLocalDateTime(date: Date) = LocalDateTime.ofInstant(date.toInstant, ZoneId.systemDefault)

  private def localDateTimeToDate(localDateTime: LocalDateTime) =
    Date.from(localDateTime.atZone(ZoneId.systemDefault).toInstant)
}

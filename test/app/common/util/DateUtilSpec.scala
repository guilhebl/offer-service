package app.common.util

import java.util.Calendar

import common.UnitSpec
import common.util.DateUtil

class DateUtilSpec extends UnitSpec {

  "Mid Range date" should "be valid" in {
    val cal = Calendar.getInstance
    cal.add(Calendar.DATE, -1)
    val startDate = cal.getTime

    val cal2 = Calendar.getInstance
    cal2.add(Calendar.DATE, -1)
    cal2.add(Calendar.HOUR, 1)
    val midDate = cal2.getTime

    val cal3 = Calendar.getInstance
    val endDate = cal3.getTime

    assert(DateUtil.isWithinRange(midDate, startDate, endDate))
  }

  "Mid Range date" should "be invalid" in {
    val cal = Calendar.getInstance
    cal.add(Calendar.DATE, -1)
    val startDate = cal.getTime

    val cal2 = Calendar.getInstance
    cal2.add(Calendar.DATE, -1)
    cal2.add(Calendar.MINUTE, -1)
    val midDate = cal2.getTime

    val cal3 = Calendar.getInstance
    val endDate = cal3.getTime

    assert(!DateUtil.isWithinRange(midDate, startDate, endDate))
  }

  "Parse Date" should "be valid" in {
    val dateString = "2018-12-13T10:00:03.812Z"
    val result = DateUtil.parseLocalDateTime(dateString)
    assert(result.toString == "2018-12-13T10:00:03.812")
  }

}
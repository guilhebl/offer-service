package common.util

object RegexUtil {

  val HtmlTagsPattern = """"<(?!\/?a(?=>|\s.*>))\/?.*?>"""".r

  val UsernamePattern = "^[a-z0-9_-]{6,25}$".r

  val EmailPattern = """^[_A-Za-z0-9-]+(\\.[_A-Za-z0-9-]+)*@[A-Za-z0-9]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$""".r

  val ImagePattern = """([^\s]+(\.(?i)(jpg|png|gif|bmp))$)""".r


  val IpAddressPattern =
    """^([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.
      |([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.([01]?\\d\\d?|2[0-4]\\d|25[0-5])$""".stripMargin.r


  def filterHtmlTags(s : Option[String]) : String = {
    s match {
      case Some(str) => str.replaceAll(HtmlTagsPattern.toString(), "")
      case _ => ""
    }
  }

  def isUsernameValid(username: String): Boolean = {
    username.matches(UsernamePattern.toString())
  }

}

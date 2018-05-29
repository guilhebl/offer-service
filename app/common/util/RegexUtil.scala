package common.util

object RegexUtil {
  def filterHtmlTags(s : Option[String]) : String = {
    s match {
      case Some(str) => str.replaceAll("""<(?!\/?a(?=>|\s.*>))\/?.*?>""", "")
      case _ => ""
    }
  }
}

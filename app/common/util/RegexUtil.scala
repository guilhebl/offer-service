package common.util

object RegexUtil {
  def filterHTMLTags(s : Option[String]) : String = {
    s match {
      case Some(str) => str.replaceAll("""<(?!\/?a(?=>|\s.*>))\/?.*?>""", "")
      case _ => ""
    }
  }
}

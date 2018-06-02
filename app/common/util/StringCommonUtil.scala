package common.util

import scala.collection.mutable.ListBuffer

/**
 * Simple String Util type of class serving similar purpose as Apache Commons StringUtil class to check for empty strings.
 */
object StringCommonUtil {
  def isNotBlank(s : Option[String]) : Boolean = {
    s.exists(_.trim.nonEmpty)
  }

  def isBlank(s : Option[String]) : Boolean = {
    s.exists(str => str == null || str.trim.isEmpty)
  }

  def isBlank(s : String) : Boolean = {
    s.trim.isEmpty
  }

  /**
    * creates a list of tuples List[(String, Int)] which contains each keyword and its
    * respective count of number of matches found in the text.
    *
    * @param str
    * @param substrings
    * @return
    */
  def findAllSubstringMatchCount(str: String, substrings : Set[String]) : Seq[(String, Int)] = {
    substrings.map(k => (k -> ("(?i)\\Q" + k + "\\E").r.findAllMatchIn(str).length)).toSeq
  }

  /**
    * creates a list of tuples List[(String, Int)] which contains each keyword and its
    * respective index found in the text sorted in order by index.
    *
    * @param str
    * @param substrings
    * @return
    */
  def findAllSubstringMatches(str: String, substrings : Set[String]) : Seq[(String, Int)] = {
    val idxs = substrings.map(k => (k -> ("(?i)\\Q" + k + "\\E").r.findAllMatchIn(str).map(_.start)))
      .map{ case (keyword,itr) => itr.map((keyword, _))}
      .flatMap(identity).toSeq
      .sortBy(_._2)
    idxs
  }

  /**
    * Finds the min distance between multiple substrings in a given string
    * @param str
    * @param s
    * @return
    */
  def getMinWindowSize(str : String, substrings : Set[String]): Int = {
    val idxs = findAllSubstringMatches(str, substrings)

    // Calculates the min window on the next step.
    var min = Int.MaxValue
    var minI, minJ = -1

    // current window indexes and words
    var currIdxs = ListBuffer[Int]()
    var currWords = ListBuffer[String]()

    for(idx <- idxs ) {

      // check if word exists in window already
      val idxOfWord = currWords.indexOf(idx._1)

      if (!currWords.isEmpty && idxOfWord != -1) {
        currWords = currWords.drop(idxOfWord + 1)
        currIdxs = currIdxs.drop(idxOfWord + 1)
      }
      currWords += idx._1
      currIdxs += idx._2

      // if all keys are present check if it is new min window
      if (substrings.size == currWords.length) {
        val currMin = Math.abs(currIdxs.last - currIdxs.head)
        if (min > currMin) {
          min = currMin
          minI = currIdxs.head
          minJ = currIdxs.last
        }
      }
    }
    min
  }

}
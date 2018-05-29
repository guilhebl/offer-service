package common.util

object CollectionUtil {

  /**
    * A custom version of transpose which can handle lists of multiple lengths
    *
    * @param xs
    * @tparam A
    * @return
    */
  def transposeLists[A](xs: List[List[A]]): List[List[A]] = xs.filter(_.nonEmpty) match {
    case Nil    =>  Nil
    case ys: List[List[A]] => ys.map{ _.head }::transposeLists(ys.map{ _.tail })
  }
}

package common.util

import play.api.data.{Mapping, RepeatedMapping}

/**
  * Defines custom mappings for Scala Play forms
  */
object FormMappingUtil {
  def vector[A](mapping: Mapping[A]): Mapping[Vector[A]] = RepeatedMapping(mapping).transform(_.toVector, _.toList)
  def indexedSeq[A](mapping: Mapping[A]): Mapping[IndexedSeq[A]] = RepeatedMapping(mapping).transform(_.toIndexedSeq, _.toList)
}

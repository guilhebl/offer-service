package common.util


import scala.reflect.runtime.universe._
import scala.reflect.runtime.{universe => ru}

object ReflectionUtil {

  /**
    *  Returns the value of all Case Class fields
    * @param obj case class object
    * @return a Sequence of all field values
    */
  def getAllCaseClassFieldValues[T: ru.TypeTag](obj: Object): Seq[Any] = {
    val mirror = ru.runtimeMirror(getClass.getClassLoader)
    getMethods[T].map(m => mirror.reflect(obj).reflectField(m).get)
  }

  /**
    * Gets all methods of a case class
    * @tparam T case class type tag
    * @return
    */
  def getMethods[T: ru.TypeTag] = typeOf[T].members.collect {
    case m: MethodSymbol if m.isCaseAccessor => m
  }.toList

}

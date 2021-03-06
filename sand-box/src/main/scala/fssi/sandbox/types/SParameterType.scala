package fssi
package sandbox
package types
import fssi.contract.lib.Context

sealed trait SParameterType {
  def `type`: Class[_]
}

object SParameterType {
  case object SInt extends SParameterType {
    override def `type`: Class[_] = classOf[Int]
  }
  case object SLong extends SParameterType {
    override def `type`: Class[_] = classOf[Long]
  }
  case object SFloat extends SParameterType {
    override def `type`: Class[_] = classOf[Float]
  }
  case object SDouble extends SParameterType {
    override def `type`: Class[_] = classOf[Double]
  }
  case object SString extends SParameterType {
    override def `type`: Class[_] = classOf[String]
  }
  case object SBoolean extends SParameterType {
    override def `type`: Class[_] = classOf[Boolean]
  }

  case object SContext extends SParameterType {
    override def `type`: Class[_] = classOf[Context]
  }

  def apply(typeString: String): SParameterType = typeString match {
    case i if SInt.`type`.getSimpleName.equals(i)     => SInt
    case l if SLong.`type`.getSimpleName.equals(l)    => SLong
    case f if SFloat.`type`.getSimpleName.equals(f)   => SFloat
    case d if SDouble.`type`.getSimpleName.equals(d)  => SDouble
    case s if SString.`type`.getSimpleName.equals(s)  => SString
    case b if SBoolean.`type`.getSimpleName.equals(b) => SBoolean
    case c if SContext.`type`.getSimpleName.equals(c) => SContext
    case x =>
      throw new IllegalArgumentException(s"unsupported contract method parameter type: $x")
  }
}

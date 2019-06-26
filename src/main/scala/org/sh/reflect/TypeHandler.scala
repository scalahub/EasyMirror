
package org.sh.reflect

import java.util.ArrayList

import org.sh.utils.json.JSONUtil
import org.sh.utils.json.JSONUtil._

import scala.collection.JavaConversions._
import scala.concurrent.Await
import scala.concurrent.Future
import scala.reflect.ClassTag
import scala.reflect.runtime.universe._
import scala.reflect._
import org.sh.utils.Util._
import org.sh.utils.encoding.Hex

/**
 * TypeHandler is a class encapsulating methods for (de)serializing Scala/Java objects. That is methods for converting string to a Scala type and vice versa
 * By default the following are supported
 *
 * String
 * Int
 * Boolean
 * Array[String]
 *
 */
object DefaultTypeHandler extends TypeHandler // use this mostly. unless  processor-specific typehandler needed
class TypeHandler {
  // handledTypes is list containing:
  //    1. The type (of parameter or return value) to handle,
  //    2. The parameter handler function that converts string to correct type of 1.
  //    3. The return handler function that converts the type of 1. to a string
  private var handledTypes:ArrayList[(Class[_], String => Any, Any => String)] = new ArrayList(0)
  def getHandledTypes = handledTypes.map(_._1)
  /**
   * adds a new type to handle (i.e., (de)serialize a Scala/Java class
   *
   * T is the class to be handled
   *
   * func1 is a method that takes string and outputs an instance of T (used for user input mostly)
   * func2 is a method that takes in an instance of T and outputs a string (used for system output mostly)
   */
  def addType[T <: AnyRef](c:Class[T], func1: String => T, func2: T => String) =
    if (! handledTypes.map(_._1).contains(c)) handledTypes.add((c, func1.asInstanceOf[String => Any], func2.asInstanceOf[Any => String]))

  def addTypeTagged[Tag:TypeTag, T <: AnyRef](c:Class[T], func1: String => T, func2: T => String) =
    if (! handledTypes.map(_._1).contains(c)) handledTypes.add((c, func1.asInstanceOf[String => Any], func2.asInstanceOf[Any => String]))


  private val protectedTypes = { // protect above types from removal as they are needed for basic functionality
    initialize
    handledTypes.map(_._1)
  }
  def initialize = {
    // add primitive handlers
    /* see http://stackoverflow.com/questions/7696930/how-to-match-classes-of-boolean-types-and-boolean-types */
    handledTypes.add((java.lang.Boolean.TYPE, _ toBoolean, _ toString))
    handledTypes.add((java.lang.Double.TYPE, _ toDouble, _ toString))
    handledTypes.add((java.lang.Integer.TYPE, _ toInt, _ toString))
    handledTypes.add((java.lang.Long.TYPE, _ toLong, _ toString))
    handledTypes.add((java.lang.Float.TYPE, _ toFloat, _ toString))
    
    // TESTING BELOW 12 Oct 2017
    handledTypes.add((java.lang.Void.TYPE, _ => Unit, _ => "Ok"))
    
    // add Boxed versions of primitive types
    handledTypes.add((classOf[Double], stringToType(java.lang.Double.TYPE , _), typeToString(java.lang.Double.TYPE , _)))
    handledTypes.add((classOf[Boolean], stringToType(java.lang.Boolean.TYPE , _), typeToString(java.lang.Boolean.TYPE , _)))
    handledTypes.add((classOf[Int], stringToType(java.lang.Integer.TYPE , _), typeToString(java.lang.Integer.TYPE , _)))
    handledTypes.add((classOf[Long], stringToType(java.lang.Long.TYPE , _), typeToString(java.lang.Long.TYPE , _)))
    handledTypes.add((classOf[Float], stringToType(java.lang.Float.TYPE , _), typeToString(java.lang.Float.TYPE , _)))
    
    // add more complex types
    addType[String](classOf[String], identity , identity)
    addType[BigInt](classOf[BigInt], BigInt(_), _.toString)
    addType[BigDecimal](classOf[BigDecimal], BigDecimal(_), _.toString)
    addType[java.math.BigInteger](classOf[java.math.BigInteger], new java.math.BigInteger(_), _.toString)
    addType[java.math.BigDecimal](classOf[java.math.BigDecimal], new java.math.BigDecimal(_), _.toString)

    // COLLECTIONS
    // later on reuse 
    addType[Array[String]](classOf[Array[String]], decodeJSONArray(_).map(_ toString), encodeJSONArray(_).toString)
    addType[Array[Long]](classOf[Array[Long]], decodeJSONArray(_).map(_.toString.toLong), encodeJSONArray(_).toString)
    addType[Array[Int]](classOf[Array[Int]], decodeJSONArray(_).map(_.toString.toInt), encodeJSONArray(_).toString)
    addType[Array[Double]](classOf[Array[Double]], decodeJSONArray(_).map(_.toString.toDouble), encodeJSONArray(_).toString)
    addType[Array[Float]](classOf[Array[Float]], decodeJSONArray(_).map(_.toString.toFloat), encodeJSONArray(_).toString)

    // below added 03 June 2019
    addType[Array[Boolean]](
      classOf[Array[Boolean]],
      decodeJSONArray(_).map(_.toString.toBoolean),
      encodeJSONArray(_).toString
    )

    // below added 03 June 2019
    def hexToBytes(str:String) = {
      if (str.length % 2 == 1) throw new Exception(
        s"Hex encoded string size is odd (${str.length}). $str"
      )
      Hex.decode(str)
    }

    addType[Array[Byte]](classOf[Array[Byte]], hexToBytes, Hex.encodeBytes)

    addType[Array[Array[Byte]]](
      classOf[Array[Array[Byte]]],
      str => decodeJSONArray(str).map(_.toString).map(hexToBytes),
      arrArrByte => {
        val arrEncoded = arrArrByte.map{
          arrByte => Hex.encodeBytes(arrByte)
        }
        encodeJSONArray(arrEncoded).toString
      }
    )

    /////////////////// Option //////////////////
    def addOptType[B](func1: String => Option[B])(implicit tag:ClassTag[B], typeTag:TypeTag[B]) =
      addType[Option[B]](classOf[Option[B]], func1, o => encodeJSONArray(o.map(_.toString).toArray).toString)


    // addOptType[String](s => s match {case "None" => None:Option[String]; case any => Some(any):Option[String] })
    // addOptType(s => s match {case "None" => None:Option[Int]; case any => Some(any.toInt):Option[Int] })

    // following logic:
    //  for Option[String], in the user's input (i.e., in String => T),
    addOptType[String](s => s match {case "" => None:Option[String]; case any => Some(any):Option[String] })
    addOptType(s => s match {case "" => None:Option[Int]; case any => Some(any.toInt):Option[Int] })

    addType[java.util.List[String]](classOf[java.util.List[String]], decodeJSONArray(_).map(_ toString).toList, x => encodeJSONArray(x.toArray).toString)
    addType[java.lang.Integer](classOf[java.lang.Integer], new java.lang.Integer(_), _.toString)
  }

  /**
   * This method is to be used from Java code to add a new Java type <T> to be (de)serialized.
   *
   * c is the Class of T
   * javaType is an object from a class implementing JavaType<T>
   *
   */
  def addJavaType[T <: AnyRef](c:Class[T], javaType:JavaType[T]) = addType[T](c, javaType.stringToType, javaType.typeToString)

  /**
   * removes the type from being handled by our handler (i.e., stops (de)serializing the type)
   *
   * c is the class of the type to be removed. For instance, to remove (already added) type MyClass, use Class.forName("MyClass") as c
   */
  def removeType(c:Class[_]) = 
    if (! protectedTypes.contains(c)) handledTypes.remove(handledTypes.map(_._1).indexOf(c))
  def getParams(names:List[String], types:Array[Class[_]], jsonString:String):Array[Object] = {
    lazy val jp = getJSONParams(names, jsonString)
    names.indices.map(i => stringToType(types.apply(i), jp.apply(i)).asInstanceOf[AnyRef]).toArray
  }

  /**
   * Main method to be invoked when converting string to a Scala type (deserializing)
   */
  def stringToType[T](objectType:Class[T], s:String) = {
    handledTypes.find(_._1 == objectType) match {
      case None => 
        deserialize(s).asInstanceOf[T]
      case any => 
        any.get._2(s)
    }
  }

  /**
   * Main method to be invoked when converting a Scala type to String (serializing)
   */
  def typeToString(objectType:Class[_], a:Any):String = handledTypes.find(_._1 == objectType) match {
    case None => // throw new Exception("output: Could not get handler for "+objectType.getCanonicalName)
      // println("[INFO]: No handler for converting "+objectType.getCanonicalName+" to String; using default toString() method.")
      import scala.concurrent.ExecutionContext.Implicits.global
      import scala.concurrent.duration._
      a match {
        case f:Future[_] => 
          val result = Await.result(f, 500 millis)
          typeToString(result.getClass, result)
        case a: Array[_] => encodeJSONArray(a.map(_.toString)).toString
        case o:org.json.JSONObject =>
          o.toString
        case a: Seq[_] => encodeJSONSeq(a.map(_.toString)).toString
        case Unit => "void"
        case null => "null"
        case anyRef:JsonFormatted =>
          anyRef.toString
        case anyRef:AnyRef =>
          println(s" [Reflect:TypeToString] Error for [${anyRef.getClass}] with data: "+anyRef.toString.take(100)+"\n[end data]")
          anyRef.toString
          // commented below line on 11 Jun 2019
          // serialize(anyRef)
        case any =>
          any.toString
      }
    case any => any.get._3(a)
  }

  // following to use for Java-client <--http--> EasyProxy Server communication
  // for browser <--http--> EasyProxy Server communication, use typeToString

  def typeToStringJavaNoWeb(objectType:Class[_], a:Any):String = handledTypes.find(_._1 == objectType) match {
    case None => // throw new Exception("output: Could not get handler for "+objectType.getCanonicalName)
      // println("[INFO]: No handler for converting "+objectType.getCanonicalName+" to String; using default toString() method.")
      import scala.concurrent.ExecutionContext.Implicits.global
      import scala.concurrent.duration._
      a match {
        case f:Future[_] => 
          val result = Await.result(f, 500 millis)
          typeToString(result.getClass, result)
        case anyRef:AnyRef => 
          serialize(anyRef)
        case any => 
          //print(" typeToString: => "+any)
          any.toString
      }
    case any => any.get._3(a)
  }
  
}


/**
 * trait (interface in Java) to handle calling this library from Java.
 * Since Java does not allow passing functions as parameters, these must be encapsulated in this class.
 *
 * to add a new type from Java code, first implement the interface JavaType<T> where T is the type you want to handle
 * Then call the addJavaType method of TypeHandler
 *
 * typeToString should take in a type [T] and output a String
 * stringToType will do the opposite of typeToString
 */
trait JavaType[T] {
  def typeToString[T](t:T):String
  def stringToType[T](s:String):T
}



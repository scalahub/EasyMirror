
package org.sh.reflect
//import java.io.File
import java.util.ArrayList
import org.sh.utils.common.json.JSONUtil
import org.sh.utils.common.json.JSONUtil._
import scala.collection.JavaConversions._
import scala.concurrent.Await
import scala.concurrent.Future
import scala.reflect.ClassTag
import scala.reflect.runtime.universe._
import scala.reflect._
import org.sh.utils.common.Util._

//import scala.util.Failure
//import scala.util.Success
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
   * func1 is a method that takes string and outputs an instance of T
   * func2 is a method that takes in an instance of T and outputs a string
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
    
    /////////////////////////////////////////////////////
    /////////////////// Option //////////////////
    /////////////////////////////////////////////////////
    //  // old method below
    //  addType[Option[Any]]( 
    //    classOf[Option[Any]], s => throw new Exception("Unsupported op: String => Option"), 
    //    o => encodeJSONArray(o.toArray).toString // Option to String
    //  )
    //  // below added 03 Oct 2017
    def addOptType[B](func1: String => Option[B])(implicit tag:ClassTag[B], typeTag:TypeTag[B]) =
      addType[Option[B]](classOf[Option[B]], func1, o => encodeJSONArray(o.map(_.toString).toArray).toString)

//    def addOptTypeNew1[T: TypeTag](func1: String => Option[T]) =
//      addType[Option[T]](classOf[Option[T]], func1, o => encodeJSONArray(o.map(_.toString).toArray).toString)
//    def addOptTypeNew[T:TypeTag](func1: String => T) =
//      addType[Option[T]](
//        classOf[Option[T]], 
//        s => s match {
//          case "None" => None:Option[T]
//          case any => try Some(func1(s)):Option[T] catch {case a:Any => None:Option[T]}
//        }, 
//        o => encodeJSONArray(o.map(_.toString).toArray).toString
//      )
//    addOptTypeNew[String](s => s)
//    addOptTypeNew[Int](s => s.toInt)
//    addOptTypeNew[Long](s => s.toLong)
    
    addOptType[String](s => s match {case "None" => None:Option[String]; case any => Some(any):Option[String] })
    addOptType(s => s match {case "None" => None:Option[Int]; case any => Some(any.toInt):Option[Int] })
//    addOptType(s => s match {case "None" => None:Option[Long]; case any => Some(any.toLong):Option[Long] })
    //////////////////////////////////////////////////
    /////////////////// Option end //////////////////
    //////////////////////////////////////////////////
    
    addType[java.util.List[String]](classOf[java.util.List[String]], decodeJSONArray(_).map(_ toString).toList, x => encodeJSONArray(x.toArray).toString)
    addType[java.lang.Integer](classOf[java.lang.Integer], new java.lang.Integer(_), _.toString)

    //    addType[Set[String]](
    //      classOf[Set[String]], {x =>
    //        //println(" ---------------> "+x)        
    //        decodeJSONArray(x).map(_ toString).toSet  
    //      }
    //      , 
    //      x => encodeJSONArray(x.toArray).toString
    //    )
    //
    
    //////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////
    // following tries to reuse .. to uncomment after testing
    // COLLECTIONS
    // It is convienient to treat all collections as either a scala.collection.Traversable or scala.collection.Iterable, as these traits define the vast majority of operations on a collection.
//    def addArrayType[T](c:Class[T], t:ClassTag[T]) = addType[Array[T]](
//      classOf[Array[T]], 
//      a => decodeJSONArray(a).map(x => 
//          stringToType(c, x.toString).asInstanceOf[T]
//      ).toArray(t), 
//      x => encodeJSONArray(x.map(y => typeToString(y.getClass, y))).toString
//    )
//    def addJListType[T](c:Class[T], t:ClassTag[T]) = addType[java.util.List[T]](
//      classOf[java.util.List[T]], 
//      a => decodeJSONArray(a).map(x => 
//          stringToType(c, x.toString).asInstanceOf[T]
//      ).toList, 
//      x => encodeJSONArray(x.toArray.map(y => typeToString(y.getClass, y))).toString
//    )
//    //    addArrayType(classOf[String], classTag[String])
//    //    addArrayType(classOf[Int], classTag[Int])
//    //    addArrayType(classOf[Long], classTag[Long])
//    //    addArrayType(classOf[File], classTag[File])
//    addArrayType(classOf[Float], classTag[Float])
//    addJListType(classOf[Float], classTag[Float])
//    addJListType(classOf[Int], classTag[Int])
    //////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////
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
  def stringToTypeOld(objectType:Class[_], s:String) = {
    handledTypes.find(_._1 == objectType) match {
      //     handledTypes.find(_._1.getCanonicalName == objectType.getCanonicalName) match {
      case None => throw new Exception("input: Could not get handler for "+objectType.getCanonicalName)
      case any => any.get._2(s)
    }
  }
  //   
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
          //          import scala.async.Async._ //'async/await' macros blocks and implicits 
          //          Async {  
          //              val (f1,f2,f3) = (Future {1}, Future {2}, Future {3})
          //              val s = await {f1} + await {f2} + await {f3}
          //              println(s"Sum:  $s")
          //          } onFailure { case e => /* Handle failure */ }
          //          f.onComplete {
          //            case Success(item) => item
          //            case Failure(ex) =>
          //          }
        case a: Array[_] => encodeJSONArray(a.map(_.toString)).toString
        case o:org.json.JSONObject => o.toString
          //////////////////////////////////////////////////////////
          //////////////////////////////////////////////////////////
          //////////////////////////////////////////////////////////
          //        case c:Traversable[T] => typeToString(Array[T], a)
          //        case c:Iterable[T] => typeToString(Array[T], a)
          //////////////////////////////////////////////////////////
          //////////////////////////////////////////////////////////
          //////////////////////////////////////////////////////////
        case a: Seq[_] => encodeJSONSeq(a.map(_.toString)).toString
        //case a: Set[_] => encodeJSONSeq(a.map(_.toString).toSeq).toString
        case Unit => "void"
        case null => "null"
        case anyRef:JsonFormatted => anyRef.toString
        case anyRef:AnyRef => 
          println(s" [Reflect:TypeToString] Error for [${anyRef.getClass}] with data: "+anyRef.toString.take(100)+"\n[end data]")
          serialize(anyRef)
        case any => 
          //print(" typeToString: => "+any)
          any.toString
      }
    case any => any.get._3(a)
  }

  // following to use for Java-client <--http--> EasyProxy Server communication
  // for browser <--http--> EasyProxy Server communuication, use typeToString
  // 
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


//{"sc":"[\"{\\\"tx_hash\\\":\\\"4beff8a6d8dfae6bfd8fcb3440cbd05af61248b856a5b62fd17d61e7235c101a\\\",\\\"value\\\":7624098649,\\\"n\\\":2}\"]","address":"125NYpUEF377vRqZ6Wp7p1uvrnecWS6C7D","notifierName":"manualAudit","conf":"rO0ABXNyABFqYXZhLmxhbmcuSW50ZWdlchLioKT3gYc4AgABSQAFdmFsdWV4cgAQamF2YS5sYW5nLk51bWJlcoaslR0LlOCLAgAAeHAAAAAA"}

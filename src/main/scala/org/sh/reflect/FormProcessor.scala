package org.sh.reflect

import java.lang.reflect.Method
import Util._
import org.sh.utils.Util.serialize
import org.sh.utils.encoding.Base64
import java.io._
import org.sh.reflect.DataStructures._

/**
  * Firstly some background.
  * We use the "Web" terminology, so that a method is equivalent to a "form", which has some input boxes
  * (representing the inputs to the method) and a submit button (representing an invocation of the method).
  * Finally a form returns something, representing the output of the method.
  *
  * This is designed with Scala in mind, but should work for Java classes as well.
  *
  * A FormProcessor takes in an instance of AnyRef. Note that this can represent either an instance of
  * a class (created using the `new` keyword) or a singleton object (created using the `object` keyword).
  *
  * It scans all the methods of that object that start with some given string and then provides a
  * "virtual form" for calling those methods via a an object called EasyProxy, which acts as a proxy
  * between the user and the actual object to access.
  *
  * This allows a lot of customizations and allows for better object level access control.
  *
  * Note that each FormProcessor instance corresponds to a single AnyRef instance.
  */
/**
 * This is the main class for processing a form
 *
 * The class takes as input a startTag, a reference to an object, c, and a TypeHandler th
 *
 * @param startTag a string indicating the starting characters of the methods to make publicly available via the form processor
 * @param c a reference to any object containing such methods
 * @param th a TypeHandler object indicating the types to handle via the form processor
 * 
 * The 4th Arg Option[InputStream] is given because in some cases (when the class to be loaded is 
 * not in the current classloader's visibility, then "getMethods" method in class "FormProcessor"
 * gives a "null pointer exception" when attempting to load the class's bytecode. 
 * 
 * In this case we want to read the class's code directly from the file conatining the bytecode, 
 * which is where the 4th parameter comes in. If the class is externally loaded, use the
 * 4th parameter (either file name or inputstream of the bytecode).
 * 
 * Otherwise for normal cases, use the "typical use constructor"
 * The classFileName must be valid bytecode (generally ending with ".class").
 * This also takes a InputStream which is a stream containing the bytecode.
 */
class FormProcessor(startTag:String, c:AnyRef, th:TypeHandler, optIs:Option[InputStream], processSuperClass:Boolean) {
  def this(startTag:String, c:AnyRef, th:TypeHandler, processSuperClass:Boolean)= this(startTag, c, th, None, processSuperClass) // typical use constructor
  def this(startTag:String, c:AnyRef, th:TypeHandler)= this(startTag, c, th, None, true) // typical use constructor
  lazy val getClassName = {
    val x = c.getClass.getCanonicalName 
    if (x.endsWith("$")) x.init else x
  }
  lazy val getSimpleClassName = {
    val x = c.getClass.getSimpleName
    if (x.endsWith("$")) x.init else x
  }
  lazy val getPackageName = c.getClass.getPackage.getName
  def getTypeHandler = th
  private def displayName(m:ScalaMethod) = m.name.substring(startTag.length)
  private def actualName(displayName:String) = startTag+displayName
  private lazy val availableMethods:List[(ScalaMethod, Method)] = {
    val all = if (optIs.isDefined) { // optIs is Option[InputStream], which defines if we need to read bytecode from inputstream or not
      getMethods(c, optIs.get) 
    } else {
      getMethods(c, processSuperClass:Boolean)
    }
    all.foreach{sm => 
      if (sm.name.contains("$")) {
        println(s"[REFLECT:WARNING] Method ${sm.name} will be filtered out because its name contains a $$")
      }
    }
    
    all.filter(_.name.startsWith(startTag)).filterNot(_.name.contains("$")).collect(x => // This lines filters out methods with names containing $
      getJavaMethod(c, x) match {
        case Some(m) => (x, m)
      }
    ).groupBy(_._1.name).toList.flatMap(x =>  // for method with duplicate name, append index to method name
      x match {
      case (k, v) if v.size > 1 => 
        v.indices.map(i => {          
            val sm = v(i)._1
            (ScalaMethod(k+(i+1).toString, sm.returnType,sm.params, sm.parentClassName, k, sm.methodInfo, sm.groupInfo, sm.infoVars), v(i)._2)
          }
        )
      case (k, v) => v
    }
  )
  }
  
  def processForm(formName:String, jsonString:String)(implicit sessionSecret:Option[String] = None) = {
    val (result, retType) = processFormJavaObjectOutput(formName, jsonString)
    th.typeToString(retType, result)
  }
  def processFormForSerializableOutput(formName:String, jsonString:String)(implicit sessionSecret:Option[String] = None) = serialize(processFormJavaObjectOutput(formName, jsonString)._1)

  def processFormJavaObjectOutput(formName:String, jsonString:String)(implicit sessionSecret:Option[String] = None) = availableMethods.find(_._1.name == actualName(formName)) match {
    // http://stackoverflow.com/questions/5837698/converting-any-object-to-a-byte-array-in-java
    case Some((s:ScalaMethod, m:Method)) =>
      val objInstance: AnyRef = if (c.isInstanceOf[EasyMirrorSession]) {
         c.asInstanceOf[EasyMirrorSession].$setSession(sessionSecret)
      } else c

      val (x, y) = (
        m.invoke(
          objInstance,
          th.getParams(
            s.params.map(p => p.paraName),
            m.getParameterTypes, jsonString
          ): _*
        ),
        m.getReturnType
      )
      (x, y)
    case any => throw new Exception(formName+": no method found")
  }

  def getPublicMethods:List[ScalaMethod] = {
    availableMethods.map(x => x._1.getModifiedMethod(displayName(x._1), c.getClass.getCanonicalName))
  }

  def getPublicMethodsTypes = availableMethods.map(x => (displayName(x._1), x._2.getReturnType, x._2.getParameterTypes))

  // Following are "meta" methods. They return information about the current
  // object loaded in this instance of FormProcessor.
  // They return information about the methods available to invoke via the FormProcessor
  def ____getMethodsInJava:Array[String] = getPublicMethods.map(_.toJavaString).toArray
  def ____getMethodsInScala:Array[String] = getPublicMethods.map(_.toScalaString).toArray
  def ____getMethodInJava(name:String) = getPublicMethods.filter(_.name == name).map(_.toJavaString).toArray
  def ____getMethodInScala(name:String) = getPublicMethods.filter(_.name == name).map(_.toScalaString).toArray
}




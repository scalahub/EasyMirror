
package org.sh.reflect

import org.sh.reflect.DataStructures.ScalaMethod
//import scala.jdk.CollectionConverters._

object CodeGenUtil {
  def getFormMethods(fp:FormProcessor) = fp.getPublicMethods.zip(fp.getPublicMethodsTypes.map(_._2))
  
  def validateParamTypes(methods:List[(ScalaMethod, Class[_])]) =
    methods foreach(x => x._1.params foreach(y => {
          if (!exactMatch(y.paraType.getClassName)) {
            println("Error: method "+x._1.parentClassName+"#"+x._1.name+" takes unsupported type: "+y.paraType+" for parameter: "+y.paraName)
            throw new Exception("method "+x._1.parentClassName+"#"+x._1.name+" takes unsupported type: "+y.paraType+" for parameter: "+y.paraName)
          }
        }
      )      
    )

  //  is below used anywhere?
  def filterReturnTypes(methods:List[(ScalaMethod, Class[_])]) = methods filter  (x => exactClassMatch(x._2))

  def validateReturnTypes(methods:List[(ScalaMethod, Class[_])]) =
    methods foreach(x => {
        if (!exactClassMatch(x._2)){
          println("Error: method "+x._1.parentClassName+"#"+x._1.name+" returns unsupported type: "+x._1.returnType)
          throw new Exception("method "+x._1.name+" returns unsupported type: "+x._1.returnType)
        }
      }
    )
  //////////////////////////////////////////////////////////
  // added following to match using type handler rather than strings (11/Dec/2015)
  def exactClassMatch(st:Class[_]) = DefaultTypeHandler.getHandledTypes.find(_ == st) match {
    case Some(_) => true
    case _ => false
  }
  //////////////////////////////////////////////////////////

  def subst(st:String) = javaTypes.foldLeft(st)((x, y) => x.replace(y._1, y._2))
  // following needed for matching ASM return types
  def exactMatch(st:String) = javaTypes.find(_._1 == st) match {
    case Some((a, b)) => true
    case _ => false
  }
  val javaTypes = List(("java.lang.String[]", "Array[String]"), // DO NOT USE MAP. Map does not maintain ordering. Here ordering is important!!
                       ("java.lang.String", "String"), 
                       ("String[]", "Array[String]"),
                       // ("void", "Unit"),
                       ("double[]", "Array[Double]"),
                       ("boolean[]", "Array[Boolean]"),
                       ("long[]", "Array[Long]"),
                       ("int[]", "Array[Int]"),
                       ("boolean", "Boolean"),
                       ("double", "Double"),
                       ("int", "Int"), ("long", "Long"))
  def preamble(c:AnyRef) =
"""
======== auto-generated code ========
WARNING !!! Do not edit! 
Otherwise your changes may be lost the next time this file is generated.
  
Instead edit the file that generated this code. Refer below for details

Class that generated this code: """+c.getClass.getCanonicalName+"""

Stacktrace of the call is given below:

"""+
  Thread.currentThread.getStackTrace.drop(1).take(10).map{x =>
    x.getClassName+":"+x.getFileName+":"+x.getLineNumber+"\n"
  }.mkString


}
// {var st = "\n"; try {(4 to 20).foreach(x => st = st+ "\n"+sun.reflect.Reflection.getCallerClass(x).getName()); st } catch { case any:Throwable => st }; st }


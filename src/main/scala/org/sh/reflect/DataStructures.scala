
package org.sh.reflect

import org.sh.utils.common.json.JSONUtil
import org.sh.utils.common.json.JSONUtil.JsonFormatted
import org.objectweb.asm.Type

object DataStructures {

  case class Param(paraName:String, paraType:Type) {
    def ==(p:Param) = {
      paraName == p.paraName && paraType.equals(p.paraType)
    }
    // override def toString = ""
    override def toString = s"$paraName:$paraType"
    var info:Option[String] = None
  }
  case class ScalaMethod(
    name:String, returnType:Type, params:List[Param], parentClassName:String, origName:String, 
    methodInfo:Option[String]=None, groupInfo:Option[String]=None, 
    infoVars:Map[String, String]
  ) extends JsonFormatted {

    def getModifiedMethod(newDisplayName:String, newParentClassName:String) = {
      ScalaMethod(newDisplayName, returnType, params, newParentClassName, origName, methodInfo, groupInfo, infoVars)
    }
    def cleanReturnedClassName = cleanClassName(returnType.getClassName)
    def cleanClassName(n:String) = n.replace("java.lang.String", "String")
    def ==(m:ScalaMethod) = try {
      name == m.name && returnType.equals(m.returnType) && params.indices.foldLeft(true)((x, i) => params(i) == m.params(i))
    } catch { case _:Throwable => false}
    def toScalaString:String = "def "+toScalaSignatureString+": "+cleanClassName(returnType.getClassName)
    def toScalaParamString = {
      val p = params.map(x=> x.paraName+":"+cleanClassName(x.paraType.getClassName))
      (if (p.size > 0) "("+p.reduceLeft(_+", "+_)+")" else "")
    }
    def toScalaSignatureString = name + toScalaParamString

    def canonicalSignature = {
      ////  canonical way to compare if two methods are equal in JVM 
      // We check: 
      //  method names
      //  parameter types
      //  parameter ordering
      //  
      // We DO NOT check:
      //  parameter names
      //  return types
      //  
      // Thus,
      //    def foo(a:Bar):Int  
      //      will be "equal" (in the sense of JVM signatures) to      
      //    def foo(bar:Bar):String 
      //      
      name+":"+(if (params.size > 0) params.map(_.paraType.toString).reduceLeft(_+":"+_) else "") 
    }
      
    def toJavaString:String = {
      val p = params.map(x=> cleanClassName(x.paraType.getClassName) + " "+x.paraName)
      cleanClassName(returnType.getClassName) +" "+ name+"("+
      (if (p.size > 0) p.tail.foldLeft(p.head)( (x, y) => x+","+y ) else "") +")"
    }
    val (paraNames, paraTypes) = params.toArray.map(p => (p.paraName, p.paraType)).unzip
    val keys = Array("name", "returnType", "params", "parentClassName", "origName", "methodInfo")
    val vals = Array[Any](name:String, returnType:Type, JSONUtil.createJSONString(paraNames, paraTypes), parentClassName:String, origName:String, methodInfo.getOrElse("None"))
  }
}





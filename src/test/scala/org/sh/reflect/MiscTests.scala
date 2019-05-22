
package org.sh.reflect

object JSONArrayTest {
  val ar = Array("hello", "world")
  val a = org.sh.utils.common.json.JSONUtil.encodeJSONArray(ar)
  println (a)
  val b = org.sh.utils.common.json.JSONUtil.decodeJSONArray(a.toString)
  b.map(x => println (x))
}

object FormProcessorTest extends App {
  val m = org.sh.reflect.Util.getMethods(org.sh.reflect.test.MyObject, true)
  m.foreach(x => org.sh.reflect.Util.printMethod(x))
}

object RunMethodSpyJava {
  def mainOld(arg:Array[String]) = MethodSpy.main("org.sh.reflect.test.MethodSpy", "m1")
}


//  var handledTypes:ArrayList[(Class[_], String => Any, Any => String)] = new ArrayList(0)
//  handledTypes.add((classOf[java.lang.String], x => x, x => x.asInstanceOf[String]))
//  handledTypes.add((java.lang.Boolean.TYPE, x => x.toBoolean, x => x.asInstanceOf[Boolean].toString))
//  handledTypes.add((classOf[java.security.PublicKey], x => gmws.common.Crypto.pubKeyFromBase64DERString(x),
//                   x => gmws.common.encoding.Base64.encodeBytes(x.asInstanceOf[java.security.PublicKey].getEncoded)))
//  handledTypes.add((classOf[gmws.common.Defs.Status],
//                   x => throw new Exception("Not implemented"),
//                   x => x.asInstanceOf[Status].text))
////java.lang.String[]
////------------------
////java.lang.String[]
////gmws.client.groupclient.ChatInfo
////gmws.common.Defs.Status
////java.lang.String
////boolean
////gmws.client.Defs.GroupMemberNicks
//
//  def stringToType(objectType:Class[_], s:String) = handledTypes.find(x => x._1 == objectType) match {
//    case None => throw new Exception("input: Could not get handler for "+objectType.getCanonicalName)
//    case any => any.get._2(s)
//  }
//  def typeToString(objectType:Class[_], a:Any) = handledTypes.find(x => x._1 == objectType) match {
//    case None => throw new Exception("output: Could not get handler for "+objectType.getCanonicalName)
//    case any => any.get._3(a)
//  }
//  def main(a:Array[String]):Unit = {
////  var a = 1
////  a.asInstanceOf[AnyRef].getClass.getMethods.foreach(m => m.getParameterTypes)
//
//  var a = MTGMC
////  a.asInstanceOf[AnyRef].getClass.getMethods.foreach(m => if (m.getName.startsWith("$_")){print ("\n"+m.getName+":"); m.getParameterTypes.foreach(t => print(t.getName+","))})
////  a.asInstanceOf[AnyRef].getClass.getMethods.foreach(m => if (m.getParameterTypes.length > 0){print ("\n"+m.getName+":"); m.getParameterTypes.foreach(t => print(t.getName+","))})
////  a.asInstanceOf[AnyRef].getClass.getMethods.foreach(m => if (m.getParameterTypes.length > 0){print ("\n"+m.getName+":"); m.getParameterTypes.foreach(t => print(t+","))})
//  object aa {
//    def f1(a:Int, b:String, c:Array[String], d:Array[Byte]):Int = 1
//    def f2(a:Int, b:String, c:Array[String], d:Array[Byte]):String = null
//    def f3(a:Int, b:String, c:Array[String], d:Array[Byte]):Array[String] = null
//    def f4(a:Int, b:String, c:Array[String], d:Array[Byte]):Boolean = true
//    def f5(a:Int, b:String, c:Array[String], d:Array[Byte]):java.security.PublicKey = null
//  }
//  //"getSimpleName"
//  a.asInstanceOf[AnyRef].getClass.getMethods.foreach(m => if (m.getName.startsWith("$_")){print ("\n"+m.getName+"=>"); print(m.getReturnType.getSimpleName)})
//  //"getCanonicalName:"
//  a.asInstanceOf[AnyRef].getClass.getMethods.foreach(m => if (m.getName.startsWith("$_")){print ("\n"+m.getName+"=>"); print(m.getReturnType.getCanonicalName)})
//  //"getName:"
//  a.asInstanceOf[AnyRef].getClass.getMethods.foreach(m => if (m.getName.startsWith("$_")){print ("\n"+m.getName+"=>"); print(m.getReturnType.getName)})
//  //"toString:"
//  a.asInstanceOf[AnyRef].getClass.getMethods.foreach(m => if (m.getName.startsWith("$_")){print ("\n"+m.getName+"=>"); print(m.getReturnType)})
//  }
//}

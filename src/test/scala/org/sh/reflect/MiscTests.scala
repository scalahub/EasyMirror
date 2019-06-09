
package org.sh.reflect

object JSONArrayTest {
  val ar = Array("hello", "world")
  val a = org.sh.utils.json.JSONUtil.encodeJSONArray(ar)
  println (a)
  val b = org.sh.utils.json.JSONUtil.decodeJSONArray(a.toString)
  b.map(x => println (x))
}

object FormProcessorTest extends App {
  val m = org.sh.reflect.Util.getMethods(MyObject, true)
  m.foreach(x => org.sh.reflect.Util.printMethod(x))
}

object RunMethodSpyJava {
  def mainOld(arg:Array[String]) = MethodSpy.main("org.sh.reflect.test.MethodSpy", "m1")
}




package org.sh.reflect

import org.sh.utils.common.json.JSONUtil
import org.sh.reflect.Util._

object ProxyTest extends App{
  val th = DefaultTypeHandler
  // example to add type handler 
  // th.addType[PublicKey](classOf[PublicKey], pubKeyFromBase64DERString , x => Base64.encodeBytes(x.getEncoded))
  org.sh.reflect.Util.debug = true
  
  th.addType[D](
    classOf[D], 
    s => {
      val ar = JSONUtil.decodeJSONArray(s)
      D(ar(0).asInstanceOf[String], ar(1).asInstanceOf[Int])
    }, 
    d => JSONUtil.encodeJSONArray(Array(d.s, d.i)).toString 
  )
  import EasyProxy._
  val myObjectPID = "myObject"
  EasyProxy.addProcessor(myObjectPID, "__test__", MyObject, th, false)
  assert(metaPid(myObjectPID) == myObjectPID+"Meta")
  
  val myClassPID = "myClass"
  EasyProxy.addProcessor(myClassPID, "", new MyClass, th, false)
  assert(metaPid(myClassPID) == myClassPID+"Meta")

  // prints the methods available in EasyProxy for processor ID myObjectPID
  EasyProxy.getProcessor(myObjectPID).map{p =>
    p.getPublicMethods.foreach(printMethod _ )
  }
  // prints the methods available in EasyProxy for processor ID myClassPID
  EasyProxy.getProcessor(myClassPID).map{p =>
    p.getPublicMethods.foreach(printMethod _ )
  }
  
  assert(
    EasyProxy.getResponse(metaPid(myObjectPID), "getMethodsInScala", "") ==
    """["def F8(a:int, b:String): int","def F1(a:int): boolean","def F4(a:String, b:String, c:String[], d:org.sh.reflect.test.D): scala.Tuple2[]","def F5(a:int, s:String, d:org.sh.reflect.test.D): org.sh.reflect.test.D[]","def F6(a:org.sh.reflect.test.D, b:org.sh.reflect.test.D, c:org.sh.reflect.test.D, s:String): org.sh.reflect.test.D","def F3(a:int[]): String[]","def F2(a:String): String","def F7(a:int): String"]"""
  )
  //  println ("Test obj 1: "+EasyProxy.getResponse(metaPid(myObjectPID), "getMethodsInScala", ""))
  assert(EasyProxy.getResponse(myObjectPID, "F1", "{'a':1}") == "true")
  assert(EasyProxy.getResponse(myObjectPID, "F2", "{'a':'a'}") == "F2_response")
  assert(EasyProxy.getResponse(myObjectPID, "F3", "{'a':[1,2,3]}") == """["3","2","1"]""")
  assert(EasyProxy.getResponse(myObjectPID, "F4", "{'a':'hi','b':'hello','c':[1,2,3],'d':['someD', 1]}") == """["(true,1)"]""")
  assert(EasyProxy.getResponse(myObjectPID, "F5", "{'a':1, s:'hi', d:['d', 3]}") == """["D(d,3)","D(hi,1)"]""")
  assert(EasyProxy.getResponse(myObjectPID, "F6", "{'a':['a', 1], b:['b', 2], c:['c', 3], s:'s'}") == """["abc",6]""")
  assert(EasyProxy.getResponse(myObjectPID, "F7", "{'a':1}") == "true")
  assert(EasyProxy.getResponse(myObjectPID, "F8", "{'a':1, b:'b'}") == "1")
  assert(EasyProxy.getResponse(metaPid(myObjectPID), "getMethodInJava", "{'name':'F6'}") == """["org.sh.reflect.test.D F6(org.sh.reflect.test.D a,org.sh.reflect.test.D b,org.sh.reflect.test.D c,String s)"]""")
  assert(EasyProxy.getResponse(metaPid(myObjectPID), "getMethodInScala", "{'name':'F6'}") == """["def F6(a:org.sh.reflect.test.D, b:org.sh.reflect.test.D, c:org.sh.reflect.test.D, s:String): org.sh.reflect.test.D"]""")
  
  assert(EasyProxy.getResponse(metaPid(myClassPID), "getMethodsInJava", "") == """["String foo1(String s)","int foo2(int s)"]""")
  assert(EasyProxy.getResponse(metaPid(myClassPID), "getMethodsInScala", "") == """["def foo1(s:String): String","def foo2(s:int): int"]""")
  assert(EasyProxy.getResponse(metaPid(myClassPID), "getMethodInJava", "{'name':'foo1'}") == """["String foo1(String s)"]""")
  assert(EasyProxy.getResponse(metaPid(myClassPID), "getMethodInScala", "{'name':'foo2'}") == """["def foo2(s:int): int"]""")
  assert(EasyProxy.getResponse("myClassMeta", "getMethodInScala", "{'name':'foo1'}") == """["def foo1(s:String): String"]""")
  assert(EasyProxy.getResponse("myClassMeta", "getMethodInScala", "{'name':'foo2'}") == """["def foo2(s:int): int"]""")
  assert(EasyProxy.getResponse(myClassPID, "foo1", "{'s':'Alice'}") == """hello Alice""")
  assert(EasyProxy.getResponse(myClassPID, "foo2", "{'s':5}") == """15""")

  println("Äll tests passed")
}

case class D(s:String, i:Int)

object MyObject {
  def __test__F1(a:Int):Boolean = {
    val $INFO$ = "32r23"
    val foo = "123"
    true
  }
  def __test__F2(a:String):String = "F2_response"
  def __test__F3(a:Array[Int]):Array[String] = a.map(_.toString).reverse
  def __test__F4(a:String, b:String, c:Array[String], d:D):Array[(Boolean, Int)] = Array((true, 1))
  def __test__F5(a:Int, s:String, d:D):Array[D] = Array(d, D(s, a))
  def __test__F6(a:D, b:D, c:D, s:String):D = D(a.s+b.s+c.s, a.i+b.i+c.i)
  def __test__F7(a:Int):String = "true"
  def __test__F8(a:Int, b:String):Int = 1
  def test_F9(a:Int, b:Int) = a + b
  def test_F0(a:Int, b:Int) = {
    val x = "randomX"
    val $INFO$ = "method info"
    val $a$ = "a info"
    val $b$ = "b info"
    val $c$ = 123
    val y = "randomY"
    a + b
  } // won't be available
}

class MyClass {
  def foo(s:String) = "hello "+s
  def foo(s:Int) = s + 10
}
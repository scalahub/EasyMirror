package org.sh.reflect

import org.sh.reflect.TestVectors.testVectors
import org.sh.utils.common.json.JSONUtil
import org.sh.reflect._

object ProxyProxyServer {
  val pid = "ProxyProxy"
  EasyProxy.addProcessor(pid, "__", this, DefaultTypeHandler, true)

  def __getResponse(pid:String, reqName:String, reqData:String) = 
    EasyProxy.getResponse(pid, reqName, reqData)
  def getProxyReqJSON(pid:String, reqName:String, reqData:String) = 
    JSONUtil.createJSONString(Array("pid", "reqName", "reqData"),Array(pid, reqName, reqData))
}

object DummyTestServer {
  val pid = "dummyTest"
  def a_foo(s:String, i:Int) = s+i
  def a_bar(s:String, i:Long) = s+i
  def a_baz(s:String) = Array("A", "b")
}

case class TestVector(pid:String, reqName:String, reqData:String, expected:String)

object TestVectors {
  val testVectors = Seq(
    TestVector(EasyProxy.metaPid(ProxyProxyServer.pid), "getMethodsInJava", "",
      """["String getResponse(String pid,String reqName,String reqData)"]"""
    ),
    TestVector(EasyProxy.metaPid(DummyTestServer.pid), "getMethodsInScala", "",
      """["def foo(s:String, i:int): String","def bar(s:String, i:long): String","def baz(s:String): String[]"]"""
    ),
    TestVector(EasyProxy.metaPid(ProxyProxyServer.pid), "getMethodInScala", "{'name':'getResponse'}",
      """["def getResponse(pid:String, reqName:String, reqData:String): String"]"""
    ),
    TestVector(EasyProxy.metaPid(DummyTestServer.pid), "getMethodsInJava", "",
      """["String foo(String s,int i)","String bar(String s,long i)","String[] baz(String s)"]"""
    ),
    TestVector(DummyTestServer.pid, "foo", "{'s':'hello', 'i':'3'}", """hello3"""),
    TestVector(ProxyProxyServer.pid,"getResponse",
      "{'pid':'"+DummyTestServer.pid+"','reqName':'foo','reqData':'{\\'s\\':\\'hello\\', \\'i\\':\\'3\\'}'}",
      """hello3"""
    ),
    TestVector(ProxyProxyServer.pid,"getResponse",
      s"""{'pid':'${DummyTestServer.pid}','reqName':'foo','reqData':'{\\'s\\':\\'hello\\', \\'i\\':\\'3\\'}'}""",
      """hello3"""
    ),
    TestVector(ProxyProxyServer.pid, "getResponse",
      ProxyProxyServer.getProxyReqJSON(DummyTestServer.pid, "foo", "{'s':'hello', 'i':'3'}"),
      """hello3"""
    )
  )
}

// following object tests above ProxyProxyServer.
object TestProxyProxyServer extends App {
  // do we need to test the below line?
  EasyProxy.addProcessor(DummyTestServer.pid, "a_", DummyTestServer, DefaultTypeHandler, false)
  testVectors.foreach{
    case TestVector(pid, reqName, reqData, expected) =>
      val actual = EasyProxy.getResponse(pid, reqName, reqData)
      assert(actual == expected, s"Expected: $expected. Actual: $actual")
  }
  println("Tests passed for pid: "+ProxyProxyServer.pid)
}


// The ProxyQueryMaker (below) cannot be tested here because it needs an implementation of QueryMaker
// The implementations are avalable in ReflectSocket and ReflectWeb, where ProxyQueryMaker can be tested
class ProxyQueryMaker(qm:QueryMaker) extends QueryMaker{
  def makeQuery(pid:String, reqName:String, reqData:String) =
    qm.makeQuery(ProxyProxyServer.pid, "getResponse", ProxyProxyServer.getProxyReqJSON(pid, reqName, reqData))
  def isConnected = qm.isConnected
}

// The ProxyQueryMakerTest (below) cannot be run here because it needs an implementation of QueryMaker
// The implementations are avalable in ReflectSocket and ReflectWeb, where ProxyQueryMaker can be tested
class ProxyQueryMakerTest(qm:QueryMaker) {
  val pqm = new ProxyQueryMaker(qm)
  testVectors.foreach{
    case TestVector(pid, reqName, reqData, expected) =>
      val actual = pqm.makeQuery(pid, reqName, reqData)
      assert(actual == expected, s"Expected: $expected. Actual: $actual")
  }
  println("Peoxy tests passed for pid: "+ProxyProxyServer.pid)
}


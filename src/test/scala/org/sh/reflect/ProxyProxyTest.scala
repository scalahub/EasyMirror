package org.sh.reflect

import org.sh.utils.common.json.JSONUtil
import org.sh.reflect._

object ProxyProxyServer {
  val pid = "ProxyProxy"
  Proxy.addProcessor(pid, "__", this, DefaultTypeHandler, true)

  def __getResponse(pid:String, reqName:String, reqData:String) = 
    Proxy.getResponse(pid, reqName, reqData)
  def getProxyReqJSON(pid:String, reqName:String, reqData:String) = 
    JSONUtil.createJSONString(Array("pid", "reqName", "reqData"),Array(pid, reqName, reqData))
}
object DummyTestServer {
  val pid = "dummyTest"
  def a_foo(s:String, i:Int) = s+i
  def a_bar(s:String, i:Long) = s+i
  def a_baz(s:String) = Array("A", "b")
}
// The ProxyQueryMaker (below) cannot be tested here because it needs an implementaton of QueryMaker
// The implementations are avalable in CommonReflectSocket and CommonReflectWeb, where ProxyQueryMaker can be tested
class ProxyQueryMaker(qm:QueryMaker) extends QueryMaker{
  def makeQuery(pid:String, reqName:String, reqData:String) =
    qm.makeQuery(ProxyProxyServer.pid, "getResponse", ProxyProxyServer.getProxyReqJSON(pid, reqName, reqData))
  def isConnected = qm.isConnected
}


// following object tests above ProxyProxyServer. 
object TestProxyProxyServer extends App {
  println("testing server: "+ProxyProxyServer.pid)
// do we need to test the below line?
  Proxy.addProcessor(DummyTestServer.pid, "a_", DummyTestServer, DefaultTypeHandler, false)
  
  println ("1s => "+Proxy.getResponse(Proxy.metaPid(ProxyProxyServer.pid), "getMethodsInJava", ""))
  println ("2s => "+Proxy.getResponse(Proxy.metaPid(DummyTestServer.pid), "getMethodsInScala", ""))
  println ("3s => "+Proxy.getResponse(Proxy.metaPid(ProxyProxyServer.pid), "getMethodInScala", "{'name':'getResponse'}"))
  println ("4s => "+Proxy.getResponse(Proxy.metaPid(DummyTestServer.pid), "getMethodsInJava", ""))
  println ("5s => "+Proxy.getResponse(DummyTestServer.pid, "foo", "{'s':'hello', 'i':3}"))  
  println ("6s => "+Proxy.getResponse(ProxyProxyServer.pid, "getResponse",
                                     "{'pid':'"+DummyTestServer.pid+
                                     "','reqName':'foo','reqData':'{\\'s\\':\\'hello\\', \\'i\\':3}'}"))
  println ("7 => "+Proxy.getResponse(ProxyProxyServer.pid, "getResponse",
                                     ProxyProxyServer.getProxyReqJSON(DummyTestServer.pid, "foo", "{'s':'hello', 'i':3}")))
  System.exit(0)
}


class ProxyQueryMakerTest(qm:QueryMaker) {
  val pqm = new ProxyQueryMaker(qm)
  println ("1q => "+pqm.makeQuery(Proxy.metaPid(ProxyProxyServer.pid), "getMethodsInJava", ""))
  println ("2q => "+pqm.makeQuery(Proxy.metaPid(DummyTestServer.pid), "getMethodsInScala", ""))
  println ("3q => "+pqm.makeQuery(Proxy.metaPid(ProxyProxyServer.pid), "getMethodInScala", "{'name':'getResponse'}"))
  println ("4q => "+pqm.makeQuery(Proxy.metaPid(DummyTestServer.pid), "getMethodsInJava", ""))
  println ("5q => "+pqm.makeQuery(DummyTestServer.pid, "foo", "{'s':'hello', 'i':3}"))  
  println ("6q => "+pqm.makeQuery(ProxyProxyServer.pid, "getResponse",
                                 "{'pid':'"+DummyTestServer.pid+
                                 "','reqName':'foo','reqData':'{\\'s\\':\\'hello\\', \\'i\\':3}'}"))
  println ("7 => "+pqm.makeQuery(ProxyProxyServer.pid, "getResponse",
                                 ProxyProxyServer.getProxyReqJSON(DummyTestServer.pid, "foo", "{'s':'hello', 'i':3}")))
  System.exit(0)
}
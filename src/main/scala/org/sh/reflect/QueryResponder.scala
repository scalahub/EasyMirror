
package org.sh.reflect

trait QueryResponder {
  def getResp(pid:String, reqName:String, reqDataJson:String, useJavaSerialization:Boolean) = Proxy.getResponse(pid, reqName, reqDataJson, useJavaSerialization)  
  def getRespJavaObject(pid:String, reqName:String, reqDataJson:String) = Proxy.getResponseJavaObject(pid, reqName, reqDataJson)  
}

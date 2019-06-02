
package org.sh.reflect

trait QueryResponder {
  def getResp(pid:String, reqName:String, reqDataJson:String, useJavaSerialization:Boolean) = EasyProxy.getResponse(pid, reqName, reqDataJson, useJavaSerialization)
  def getRespJavaObject(pid:String, reqName:String, reqDataJson:String) = EasyProxy.getResponseJavaObject(pid, reqName, reqDataJson)
}

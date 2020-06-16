
package org.sh.reflect

object QueryResponder {
  def getResp(pid:String, reqName:String, reqDataJson:String, useJavaSerialization:Boolean)(implicit sessionSecret:Option[String] = None) =
    EasyProxy.getResponse(pid, reqName, reqDataJson, useJavaSerialization)
  def getRespJavaObject(pid:String, reqName:String, reqDataJson:String) = EasyProxy.getResponseJavaObject(pid, reqName, reqDataJson)
}


package org.sh.reflect.web

import org.sh.utils.common.encoding.Base64
import org.sh.utils.common.json.JSONUtil._

class Text(s:String) {
  val getText = new String(Base64.decode(s))
  override def toString = getText
}

class HTML(text:String) {
  val getHTML = text
  override def toString = getHTML
}

object WebDataStructures {
  case class Req(reqID:String, pid:String, reqName:String, reqData:String)  extends JsonFormatted {
    assert(reqID != null && pid != null && reqData != null)
    val vals:Array[Any] = Array(reqID:String, pid:String, reqName:String, reqData:String)
    val keys = Array("reqID":String, "pid":String, "reqName":String, "reqData":String)
    def getReqDataKeys = try getJSONKeys(reqData) catch {
      case e:Throwable => 
        if (org.sh.reflect.Util.debug) e.printStackTrace
        Nil
        
    }
  }
  case class Resp(reqID:String, pid:String, reqName:String, respData:String) extends JsonFormatted {
    val vals:Array[Any] = Array(reqID, reqName, respData)
    val keys = Array("reqID", "reqName", "respData")
    private val key = keys // not sure why key was used. If not used, delete it
  }
}

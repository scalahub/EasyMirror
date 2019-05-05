package mux.reflect

import java.io._
import Util._
//import amit.common.json.JSONUtil
//import java.lang.reflect.InvocationTargetException
import java.lang.reflect.InvocationTargetException
import scala.collection.mutable.{Set => MSet}

object Proxy {
  val deny = MSet[String]() // prevent these methods
  def preventMethod(m:String) = {
    deny += getRegEx(m)
  }
  
  def metaPid(pid:String)= pid+"Meta"

  private var processors: List[(String, FormProcessor)] = Nil
  def addProcessor(startTag:String, any:AnyRef, th:TypeHandler, processSuperClass:Boolean):Unit = 
    addProcessor(startTag, any, th, None, false, processSuperClass:Boolean)
  def addProcessor(startTag:String, any:AnyRef, th:TypeHandler, is:Option[InputStream], reload:Boolean, processSuperClass:Boolean):Unit = 
    addProcessor(getDefaultPid(any), startTag, any, th, is, reload, processSuperClass:Boolean)
  def addProcessor(pid:String, startTag:String, any:AnyRef, th:TypeHandler, processSuperClass:Boolean):Unit = 
    addProcessor(pid, startTag, any, th, None, false, processSuperClass:Boolean)
  def addProcessor(pid:String, startTag:String, any:AnyRef, th:TypeHandler, reload:Boolean, processSuperClass:Boolean):Unit = 
    addProcessor(pid, startTag, any, th, None, reload, processSuperClass:Boolean)
  
  def addProcessor(startTag:String, any:AnyRef, processSuperClass:Boolean):Unit = addProcessor(startTag, any, DefaultTypeHandler, processSuperClass:Boolean)
  
  def addProcessor(pid:String, startTag:String, any:AnyRef, th:TypeHandler, optIs:Option[InputStream], reload:Boolean, processSuperClass:Boolean):Unit = {
    if (Util.debug) println ("[REFLECT:DEBUG] adding processor "+pid+":"+startTag+":"+any)
    if (! processors.map(_._1).contains(pid)) {
      val fp = new FormProcessor(startTag, any, th, optIs, processSuperClass)
      processors = (pid, fp) :: processors
      processors = (metaPid(pid), new FormProcessor(defaultMetaPrefix, fp, DefaultTypeHandler, processSuperClass)) :: processors
      // commented below line because added DefaultTypeHandler. (why?) because we need a single place to add new types to be handled.
      // Hence instead of creating a new instance each time, we use one object, DefaultTypeHandler
      // // leaving line below for reference
      //processors = (metaPid(pid), new FormProcessor(defaultPrefix, fp, new TypeHandler)) :: processors
    } else if (reload) {
      removeProcessor(pid)
      addProcessor(pid, startTag, any, th, optIs, reload, processSuperClass:Boolean)
    } else throw new Exception("Processor with PID: "+pid+" exists.")
  } 
  
  def removeProcessor(pid:String) =
    (processors.find(_._1 == pid), processors.find(_._1 == metaPid(pid))) match {
      case (Some(_), Some(_)) => processors = processors.filter(x => x._1 != pid && x._1 != metaPid(pid))
      case _ => throw new Exception("Processor with PID: "+pid+" not found.")
    }

  def getProcessor(pid:String):Option[FormProcessor] = processors.find(_._1 == pid) match {
    case None => None
    case any => Some(any.get._2)
  }
  def usingDeny[T](fp:FormProcessor, formName:String)(f: => T) = {
    val mSig = fp.getClassName+"."+formName
    if (deny.exists(mSig.matches)) throw new Exception("access denied ["+mSig+"]") else f
  }

  def getRegEx(wc:String) = wc.replace(".", "\\.").split("\\*", -1).reduceLeft((x, y) => x + ".*"+ (if (y != "") "("+y+")" else ""))
  //  def getRegEx(wc:String) = wc.split("\\*", -1).reduceLeft((x, y) => x + ".*"+ (if (y != "") "("+y+")" else ""))

  def getExceptionStack(e:Throwable):String = e match {
    case e:InvocationTargetException => getExceptionStack(e.getCause)
    case e:ExceptionInInitializerError => getExceptionStack(e.getCause)  // ? 
    case e:Throwable => 
      val cause = e.getCause
      e.getClass.getSimpleName+":"+
      (if (cause == null) e.getMessage else " caused by: "+getExceptionStack(cause))
  }
  
  // added new field below useJavaSerlializedOutput
  // if this is true, then we will use Java's serialization else we will use type handler
  
  def getResponse(pid:String, formName:String, reqDataJSON:String, useJavaSerlializedOutput:Boolean = false):String = getProcessor(pid) match {      
    case Some(fp:FormProcessor) => usingDeny(fp, formName) { 
        try {
          if (useJavaSerlializedOutput) {
            fp.processFormForSerializableOutput(formName, reqDataJSON) 
          }
          else {
            fp.processForm(formName, reqDataJSON) 
          }
        } 
        catch { 
          case e:Any => 
            println(s" [PROXY:ERROR] pid:$pid, formName:$formName, reqData:$reqDataJSON")
            if (debug) e.printStackTrace 
            throw ProxyException(getExceptionStack(e))
        }
      }
    case _ => throw ProxyException("Processor with PID: "+pid+" not found.")
  }
   
  //  sealed trait RespEncoding
  //  object JavaSerialized extends RespEncoding // serialized java object by converting to base64
  //  object TypeHandlerSerialized extends RespEncoding // serialized by type handlers coded in CommonReflect library
  //  object JavaObject extends RespEncoding // direct java object sent, no serialization
  def getResponseJavaObject(pid:String, formName:String, reqDataJSON:String) = getProcessor(pid) match {      
    case Some(fp:FormProcessor) => usingDeny(fp, formName) { 
        try {
            fp.processFormJavaObjectOutput(formName, reqDataJSON)._1
        } catch { 
          case e:Throwable => 
            if (debug) e.printStackTrace
            //throw e //ProxyException(getExceptionStack(e))
            //throw ProxyException(getExceptionStack(e))
            throw ProxyException(e.getMessage)
        }
      }
    case _ => throw ProxyException("Processor with PID: "+pid+" not found.")
  }
}
case class ProxyException(m:String) extends Exception(m) 


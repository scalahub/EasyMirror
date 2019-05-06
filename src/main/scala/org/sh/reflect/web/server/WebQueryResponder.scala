
package org.sh.reflect.web.server

//import java.io.InputStreamReader
//import java.lang.reflect.InvocationTargetException
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.OutputStreamWriter
import java.nio.file.Path
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import javax.servlet.http.HttpServlet
import javax.servlet.http.{HttpServletRequest => HReq}
import javax.servlet.http.{HttpServletResponse => HResp}
import org.sh.reflect.DefaultTypeHandler
import org.sh.reflect.QueryResponder
import org.sh.reflect.web.WebDataStructures._
import org.sh.reflect.web.HTML
import org.sh.reflect.web.Text
import org.sh.utils.common.encoding.Base64._
import org.sh.utils.common.json.JSONUtil._
import org.sh.utils.common.Util._

class WebQueryResponder extends HttpServlet {
  import WebQueryResponder._   
  def getReq(hReq:HReq) = {
    
    //println(" ---------------> "+hReq.getParameter("reqName"))
    getReqOption(
      hReq.getParameter("reqId"), 
      hReq.getParameter("pid"), 
      hReq.getParameter("reqName"), 
      hReq.getParameter("reqData"))
  }
  override def doGet(hReq:HReq, hResp:HResp) = doPost(hReq, hResp)
  override def doPost(hReq:HReq, hResp:HResp) = {
//    println( " [WebQueryResponder] ")
    hResp.setContentType("text/plain")
    hResp.getWriter.write(getResp(getReq(hReq))(Some(hReq.getRemoteHost)))
  }
}

object WebQueryResponder extends QueryResponder {
  
  //  FileStore
  //  FileStoreNIO
  
  DefaultTypeHandler.addType[Text](classOf[Text], new Text(_), _.getText /* text => throw new Exception("not supported") */)
  
  if (FileStore.isNioMode) {
    DefaultTypeHandler.addType[File](classOf[File], FileStoreNIO.getFile, FileStoreNIO.getFileLink)
    DefaultTypeHandler.addType[Array[File]](classOf[Array[File]], files => throw new Exception("not implemented"), FileStoreNIO.getFileLinks)  

    DefaultTypeHandler.addType[Path](classOf[Path], FileStoreNIO.getPath, FileStoreNIO.getPathLink )
    DefaultTypeHandler.addType[Array[Path]](classOf[Array[Path]], files => throw new Exception("not implemented"), FileStoreNIO.getPathLinks)
    DefaultTypeHandler.addType[HTML](classOf[HTML], str => throw new Exception("not supported") /* new HTML(_) */, FileStoreNIO.getHTMLLink)  
//    FileStoreNIO
  } else {
    DefaultTypeHandler.addType[File](classOf[File], FileStore.getFile, FileStore.getFileLink)
    DefaultTypeHandler.addType[Array[File]](classOf[Array[File]], files => throw new Exception("not implemented"), FileStore.getFileLinks)  
//
    DefaultTypeHandler.addType[Path](classOf[Path], x => FileStore.getFile(x).toPath, files => throw new Exception("not implemented"))
//    DefaultTypeHandler.addType[HTML](classOf[HTML], str => throw new Exception("not supported") /* new HTML(_) */, files => throw new Exception("not implemented"))  
//    FileStore
    println(" File store using non-NIO")
  }
  // following will give problems because of type erasure. Seq[<something>] may be treated as Seq[File]
  // This happened wth Set. Can also happen with Seq.. So commenting it
  //  DO NOT USE Set(sure) or Seq(?) or List (?)
  //  DefaultTypeHandler.addType[Seq[File]](classOf[Seq[File]], files => throw new Exception("not implemented"), getFileLinks)
  //  DefaultTypeHandler.addType[Set[File]](classOf[Set[File]], files => throw new Exception("not implemented"), getFileLinks)
  //  DefaultTypeHandler.addType[Iterable[File]](classOf[Iterable[File]], files => throw new Exception("not implemented"), getFileLinks)
  println("initializing file store V1")
  
  // note reqName = 
  // reqID is an additional param for the client to sync requests. Its not needed for server. We just respond with same reqID
  // (see getReqResp below)
  // currently the client is setting reqID to be same as reqName
  // Following for web-HTML responses

  // following are additional handlers to do something with responses
  var onReq = Map[String, (Req, Option[String]) => _]() // String is source (IP address, etc)
  var onReqResp = Map[String, (Req, Resp, Option[String]) => _]() // String is source (IP address, etc)
  def addToOnReqResp(id:String, on:(Req, Resp, Option[String]) => _) = {
    if (onReqResp.contains(id)) throw new Exception("id already exists: "+id)
    onReqResp += (id -> on)
  }
  
  def addToOnReq(id:String, on:(Req, Option[String]) => _) = {
    if (onReq.contains(id)) throw new Exception("id already exists: "+id)
    onReq += (id -> on)
  }
  def removeFromOnReqResp(id:String) = {
    if (onReqResp.contains(id)) onReqResp -= id 
      else throw new Exception("id does not exist: "+id)
  }
  def getOnReqRespIDs = onReqResp.map(_._1)
  def getReqOption(reqID:String, pid:String, reqName:String, encodedReqData:String) = 
    try Some(Req(reqID, pid, reqName, new String(decode(encodedReqData))))    
    catch { case _:Throwable => None }
  def getResp(req:Req):String = getResp(Some(req))
  def getResp(req:Option[Req])(implicit reqSrc:Option[String]= None):String = encodeResp(getReqResp(req)(reqSrc))
  def getReqResp(req:Option[Req])(implicit reqSrc:Option[String]= None) = {
//    println( " [WebQueryResponder] "+req.get.reqName)

    req match {
      case Some(rq@Req(reqID, pid, reqName, reqData)) => 
        val output = try {
          onReq.foreach{case (id, on) => on(rq, reqSrc)} // don't use tryIt here because invoker may want to throw exception to deny access
          getResp(pid, reqName, reqData, false)  // false => don't use java seriailization
        } catch { 
          // // maybe add following two cases inside Proxy.getResponse ??
          //  case e:InvocationTargetException => 
          //    if (debug) e.getCause.printStackTrace
          //    "Error: "+e.getCause.getMessage
          //  case e:ExceptionInInitializerError => 
          //    if (debug) e.getCause.printStackTrace
          //    // println("This will cause NoClassDefFoundError in further calls to this class")
          //    "Error: "+e.getCause.getMessage
          case e:Throwable => 
            //if (debug) e.printStackTrace            
            "Error: "+e.getMessage
        }
        // if (debug) println("Response is: "+resp)
        // println("ResponseID is: "+reqID)
        val resp = Resp(reqID, pid, reqName, output)
        // onReqResp.foreach{case (id, on) => tryIt(on(req.get, resp, reqSrc))}
        onReqResp.foreach{case (id, on) => on(rq, resp, reqSrc)} // removed tryIt. Let invoker handler error
        Some(resp)
      case _ => None
    }
  }
  private def encodeResp(any:Option[Resp]) = any match {
    case Some(Resp(reqID, pid, reqName, respData)) =>
      // reqID+":"+encodeBytes(respData.getBytes)  // <--------- original
      // reqID+":"+encodeBytes(uncompress(compress(respData)).getBytes) <------- testing compression/decompression
      val compressed = compress(respData)
      if (debug) {
        val normal = encodeBytes(respData.getBytes).size
        val zipped = compressed.size
        println (s" [INFO:Compression:${this.getClass.getSimpleName}. Normal: ${normal}; zipped: ${zipped}; compression useful? ${normal > zipped} (${zipped.toDouble/normal})}] ")
      }
      reqID+":"+compressed
    case _ => 
      //"error:"+encodeBytes("No request found".getBytes)  // <--------- original
      "error:"+compress("No request found")
  }
  
  def compress(s:String) = compressT[Char](s, a => a.mkString)
  def uncompress(s:String) = uncompressT(s, a => a).mkString
  
//  def compressBytes(a:Array[Byte]) = compressT[Byte](a, b => b.map(_.toChar).mkString)
//  def uncompressBytes(s:String) = uncompressT[Byte](s, _.map(_.toByte))
//  
  def compressT[T](u:Seq[T], seqTToString:Seq[T] => String) = {
    val baos = new ByteArrayOutputStream
    using(new OutputStreamWriter(new GZIPOutputStream(baos))){osw =>
      osw.write(seqTToString(u))
    }
    
    new sun.misc.BASE64Encoder().encode(baos.toByteArray)    
  }
  def uncompressT[T](compressed:String, stringToSeqT:String => Seq[T]) = {
    using(new GZIPInputStream(new ByteArrayInputStream(new sun.misc.BASE64Decoder().decodeBuffer(compressed)))){is =>
      stringToSeqT(scala.io.Source.fromInputStream(is).mkString)
    }
  }  

  // following for API key response
  //  removed because added a dedicated ProxyQueryResponder. We don't need WebQueryResponder just for responding to API queries
//  def getReqRespNoCatch(pid:String, reqName:String, reqDataJson:String, useJavaSerialization:Boolean = false) = getResp(pid, reqName, reqDataJson, useJavaSerialization)
  
}

//object CompressionTest extends App {
//  import WebQueryResponder._
//  def randBytes(s:Int) = {val a = new Array[Byte](s); scala.util.Random.nextBytes(a); a}
//  val arrs = 1 to 1000 map(i => randBytes(1000))
//
//  val carrs = arrs.map(compressBytes)
//
//  val uarrs = carrs.map(uncompressBytes)
//
//
//  val res = (arrs zip uarrs).map{case (a, b) => (a zip b).map{case (x, y) => x == y}.reduceLeft(_ && _)}.reduceLeft(_ && _)
//  println("Results Passed? "+res)
//}


package mux.reflect.web.server

import java.io.File
import java.io.FileInputStream
import javax.servlet.http.HttpServlet
import javax.servlet.http.{HttpServletRequest => HReq}
import javax.servlet.http.{HttpServletResponse => HResp}
//import mux.reflect.web.WebDataStructures._
//import amit.common.encoding.Base64._
//import amit.common.json.JSONUtil._
//import amit.common.Util._

class FileUploader extends HttpServlet {
  override def doGet(hReq:HReq, hResp:HResp) = doPost(hReq, hResp)
  override def doPost(hReq:HReq, hResp:HResp) = {
    hResp.setContentType("text/plain")
    val fileID = FileUploader.putFile(hReq)
    hResp.getWriter.print(fileID)
  }
}

object FileUploader {    
  import org.apache.commons.fileupload.FileItem
  import org.apache.commons.fileupload.disk.DiskFileItemFactory
  import org.apache.commons.fileupload.servlet.ServletFileUpload
  def putFile(request:HReq):String = try {
    // val maxFileSize = Int.MaxValue
    val maxFileSize = FileStore.maxSizeBytes
    val maxMemSize = maxFileSize;
    val contentType = request.getContentType();    
    if (contentType != null && (contentType.contains("multipart/form-data"))) {
      val factory = new DiskFileItemFactory();
      factory.setSizeThreshold(maxMemSize); 
      // factory.setRepository(new File("/tmp"));
      factory.setRepository(new File(FileStore.uploadDir));
      val upload = new ServletFileUpload(factory);
      // upload.setSizeMax(-1); // for unlimited
      upload.setSizeMax(maxFileSize);

      val fileItems = upload.parseRequest(request)
      val items = fileItems.iterator;
      val i = fileItems.iterator();        
      if (i.hasNext) {
        val fi = i.next.asInstanceOf[FileItem]
        if (!fi.isFormField ()) {
          val fieldName = fi.getFieldName();
          //println("fieldName => "+fieldName)
          val fileName = fi.getName();
          if (fileName == "") {
            "No fileName for field: "+fieldName+" supplied"              
          } else {
            // val isInMemory = fi.isInMemory();
            // val sizeInBytes = fi.getSize();
            val (file, fileID) = FileStore.putNewFileAndGetID(if (fileName == null) None else Some(fileName))
            fi.write(file) ;
            //println(" == filePath: ==> "+file.getAbsolutePath)
            fileID
          }
        } else "not a file field"
      } else "no file(s) attached"
    } else "no file found"
  } catch {
    case a:Any => "error uploading file: "+a.getMessage
  }
}
class FileDownloader extends HttpServlet {
  override def doGet(hReq:HReq, hResp:HResp) = doPost(hReq, hResp)
  override def doPost(hReq:HReq, hResp:HResp) = FileDownloader.sendFile(hReq, hResp)    
}

object FileDownloader {
  def sendFile(request:HReq, response:HResp) = try {
    val fileID = request.getParameter("fileID")
    val file = FileStore.getFile(fileID)
    val fileIn = new FileInputStream(file);
            //set response headers
    response.setContentLength(file.length.toInt);
    response.setContentType("application/download"); // "text/plain"
    //response.setContentType("application/octet-stream"); // "text/plain"
    response.addHeader("Content-Disposition","attachment; filename="+file.getName);
    //    response.addHeader("Content-Disposition","attachment; filename="+file.getName());
    val out1 = response.getOutputStream();
    val outputByte = new Array[Byte](4096);
    //copy binary contect to output stream
    while(fileIn.read(outputByte, 0, 4096) != -1) out1.write(outputByte, 0, 4096);
    fileIn.close();
    out1.flush();
    out1.close();
  } catch {
    case e:Any => response.getWriter.print(e.getMessage)
  }
} 




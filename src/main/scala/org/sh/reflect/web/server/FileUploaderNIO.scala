
package org.sh.reflect.web.server

//import java.io.File
//import java.io.FileInputStream
import java.nio.file.Files
import javax.servlet.http.HttpServlet
import javax.servlet.http.{HttpServletRequest => HReq}
import javax.servlet.http.{HttpServletResponse => HResp}
//import org.sh.reflect.web.WebDataStructures._

class FileUploaderNIO extends HttpServlet {
  override def doGet(hReq:HReq, hResp:HResp) = doPost(hReq, hResp)
  override def doPost(hReq:HReq, hResp:HResp) = {
    hResp.setContentType("text/plain")
    val fileID = FileUploaderNIO.putFile(hReq)
    hResp.getWriter.print(fileID)
  }
}

object FileUploaderNIO {    
  import org.apache.commons.fileupload.FileItem
  import org.apache.commons.fileupload.disk.DiskFileItemFactory
  import org.apache.commons.fileupload.servlet.ServletFileUpload
  def putFile(request:HReq):String = try {
    // val maxFileSize = Int.MaxValue
    val maxFileSize = FileStoreNIO.maxSizeBytes
    val maxMemSize = maxFileSize;
    val contentType = request.getContentType();    
    if (contentType != null && (contentType.contains("multipart/form-data"))) {
      val factory = new DiskFileItemFactory();
      factory.setSizeThreshold(maxMemSize); 
      factory.setRepository(Files.createTempDirectory("web_uploads").toFile);
      val upload = new ServletFileUpload(factory);
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
            val (path, fileID) = FileStoreNIO.putNewPathAndGetID(if (fileName == null) None else Some(fileName))           
            
            import sun.misc.IOUtils;
            
            Files.write(path, IOUtils.readFully(fi.getInputStream, -1, false))
            // fi.write(file.toFile) ;
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
class FileDownloaderNIO extends HttpServlet {
  override def doGet(hReq:HReq, hResp:HResp) = doPost(hReq, hResp)
  override def doPost(hReq:HReq, hResp:HResp) = FileDownloaderNIO.sendFile(hReq, hResp)    
}

class HTMLDownloaderNIO extends HttpServlet {
  override def doGet(hReq:HReq, hResp:HResp) = doPost(hReq, hResp)
  override def doPost(hReq:HReq, hResp:HResp) = HTMLDownloaderNIO.sendHTML(hReq, hResp)    
}

object HTMLDownloaderNIO {
  def sendHTML(request:HReq, response:HResp) = try {
    val pathID = request.getParameter("fileID")
    val path = FileStoreNIO.getPath(pathID)
    
    val fileIn = Files.newInputStream(path);
            //set response headers
    response.setContentLength(Files.size(path).toInt);
    response.setContentType("text/html");
    val out1 = response.getOutputStream();
    val outputByte = new Array[Byte](4096);
    while(fileIn.read(outputByte, 0, 4096) != -1) out1.write(outputByte, 0, 4096);
    fileIn.close();
    out1.flush();
    out1.close();
  } catch {
    case e:Any => response.getWriter.print(e.getMessage)
  }
}
object FileDownloaderNIO {
  def sendFile(request:HReq, response:HResp) = try {
    val pathID = request.getParameter("fileID")
    val path = FileStoreNIO.getPath(pathID)
    
    val fileIn = Files.newInputStream(path);
            //set response headers
    response.setContentLength(Files.size(path).toInt);
    response.setContentType("application/download"); // "text/plain"
    //response.setContentType("application/octet-stream"); // "text/plain"
    response.addHeader("Content-Disposition","attachment; filename="+path.getFileName);
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




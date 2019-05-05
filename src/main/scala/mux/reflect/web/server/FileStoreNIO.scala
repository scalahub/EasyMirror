// used for uploading / downloading files

package mux.reflect.web.server

import org.sh.utils.common.file.Util._
import org.sh.utils.common.Util._
import org.sh.utils.common.json.JSONUtil
//import java.io.File
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributeView
import java.nio.file.attribute.BasicFileAttributes
//import mux.reflect.DefaultTypeHandler

// NOTE: This is only for storing temp files. DO NOT USE FOR App specific files.
// The only purpose of the file store is for giving a File upload and download link for java.io.File types in the HTML UI
import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;import mux.reflect.web.HTML


object FileStoreNIO {
  
  val fs = Jimfs.newFileSystem(Configuration.forCurrentPlatform);

  val maxSizeBytes = 10000000 // 10 MB  
  
  def putPath(file:Path):String = {
    val (tmpFile, fileID) = putNewPathAndGetID(Some(file.getFileName.toString))
    Files.copy(file, tmpFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING)    
    fileID
  }
  def time(path:Path) = {
    Files.readAttributes(path, classOf[BasicFileAttributes]).lastModifiedTime.toMillis
  }
  def getFile(pathID:String):File = {
    val tmp = Files.createTempFile(randomAlphanumericString(20), ".tmp")
    val path = getPath(pathID)
    Files.write(tmp, Files.readAllBytes(path)) //copy(path, tmp)    
    tmp.toFile
  }
  
  def getFileWithName(pathID:String):(File, Path) = {
    val tmp = Files.createTempFile(randomAlphanumericString(20), ".tmp")
    val path = getPath(pathID)
    Files.write(tmp, Files.readAllBytes(path)) //copy(path, tmp)    
    (tmp.toFile, path.getFileName)
  }
  
  import scala.collection.JavaConversions._
  def getPath(pathID:String):Path = {
    val saveDir = fs.getPath(shaSmall(pathID))
    val files = Files.newDirectoryStream(saveDir).filter(Files.isReadable).toArray
    if (files.size == 1) files(0) else files.sortBy(x => Files.getFileAttributeView(x, classOf[BasicFileAttributeView]).readAttributes.creationTime).head
  }
  def putNewPathAndGetID(implicit fileName:Option[String] = None):(Path, String) = {
    val fileID = randomAlphanumericString(20)
    val saveDir = fs.getPath(shaSmall(fileID))
    Files.createDirectory(saveDir)// finally {}
    if (!Files.isDirectory(saveDir)) throw new Exception("unable to create save directory") 
    val name = if (fileName.isDefined) fileName.get else randomAlphanumericString(20)
    val savedFile = saveDir.resolve(name)
    println(s"Saving to file: $savedFile")
    (savedFile, fileID)
  }  
  
  def getFileLink(file:File) = getPathLink(file.toPath)

  val jettyLink = "?fileID="
  val playLink = "/"
  val actualLink = if (FileStore.isPlayMode) playLink else jettyLink
  
  def getPathLink(path:Path):String = {
    val fileID = putPath(path)
    s"""<a target='_blank' href='/getfile$actualLink$fileID'>$fileID</a>"""    
  }
  def getHTMLLink(html:HTML):String = {
    val (path, fileID) = putNewPathAndGetID(None)
    Files.write(path, html.getHTML.getBytes)
    s"""<a target='_blank' href='/gethtml$actualLink$fileID'>$fileID</a>"""    
  }
  def getFileLinks(files:Array[File]) = getPathLinks(files.map(_.toPath))
  def getPathLinks(paths:Array[Path]) = JSONUtil.encodeJSONArray(paths.map(getPathLink)).toString
  def getPathLinks(paths:Iterable[Path]) = JSONUtil.encodeJSONArray(paths.toArray.map(getPathLink)).toString
  
  
}

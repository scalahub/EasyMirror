package org.sh.reflect.web

import org.sh.reflect.Util._
import java.io.InputStream
import org.sh.reflect.CodeGenUtil._
import org.sh.reflect.FormProcessor
import org.sh.reflect.DataStructures._
import org.sh.reflect.DefaultTypeHandler
import HTMLConstants._
import org.sh.reflect.web.server.FileStore
import org.sh.reflect.web.server.FileStoreNIO
import org.sh.utils.common.file.{Util => FUtil}

class HTMLClientCodeGenerator(initRefs: List[AnyRef], postUrl:String, appInfo:String, optIsl:Option[List[InputStream]], allowOnlyKnownTypes:Boolean, hideUnknownTypes:Boolean) {
  FileStore // just access file-store to ensure type handlers for java.io.File are added
  FileStoreNIO // just access file-store to ensure type handlers for java.io.File are added
  var c:List[AnyRef] = initRefs
  var pageTitle = "Auto Generated"
  def this(c: List[AnyRef], postUrl:String, appInfo:String, ois:Option[List[InputStream]])  = this(c, postUrl, appInfo, ois, true, false)
  def this(c: List[AnyRef], postUrl:String, appInfo:String, is:List[InputStream])  = this(c, postUrl, appInfo, Some(is))
  def this(c: List[AnyRef], postUrl:String, appInfo:String)  = this(c, postUrl, appInfo, None)
  def this(c: List[AnyRef], postUrl:String)  = this(c, postUrl, "", None)
  // uses defaultPID as html file name // used for webTest
  def autoGenerateToDefaultFile(dir:String, prefix:String=defaultPrefix):String = {
    def xorC(c1:Char, c2:Char) = {
      val str = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
      val (l, r) = (str.indexOf(c1), str.indexOf(c2))
      str.charAt((l+r) % str.size)
    }
    def xor(s1:String, s2:String) = (s1 zip s2).map(x => xorC(x._1, x._2)).mkString
    val file = c.map(getDefaultPid).reduceLeft(xor(_,_))
    autoGenerateToFile(file, dir, prefix)
  }
  // uses html file name supplied in param
  def autoGenerateToFile(fileName:String, dir:String, prefix:String=defaultPrefix)(implicit ignoreMethods:List[(String, String)] = Nil) = {
    // assert(prefix != "", "prefix cannot be empty.") // actually empty prefix is not a problem here, unlike in ServerCodeGenerator
    val fullFileName = fileName+"AutoGen"+".html"
    val file = dir+"/"+fullFileName
    println(s"[reflect] prefix = $prefix")
    println("Writing HTML file: "+file)
    org.sh.utils.common.file.Util.writeToTextFile(file, generateFilteredOut(prefix, ignoreMethods))
    fullFileName
  } 
  
  // filtered in
  def autoGenerateToFileFiltered(fileName:String, dir:String, filter:List[(String, String)], prefix:String=defaultPrefix)(implicit ignoreMethods:List[(String, String)] = Nil) = {
    // assert(prefix != "", "prefix cannot be empty.") // actually empty prefix is not a problem here, unlike in ServerCodeGenerator
    val fullFileName = fileName+"AutoGen"+".html"
    val file = dir+"/"+fullFileName
    println("Creating HTML file: "+file)
    FUtil.writeToTextFile(file, generateFilteredIn(prefix, filter))
    fullFileName
  } 
  //  def cleanClassName(s:String) = s.replace("$", "")

  def getFormDetails(prefix:String) = {
    val lOptIs = if (optIsl.isDefined) optIsl.get.map(x => Some(x)) else c.map(x => None)
    val fps = (c zip lOptIs).sortWith{(l, r) => l._1.getClass.getCanonicalName < r._1.getClass.getCanonicalName}.map (x => (new FormProcessor(prefix, x._1, DefaultTypeHandler, x._2, true), x._1))
    // following for validation of params and return types
    val allMethods = fps.flatMap(x => getFormMethods(x._1))
    if (allowOnlyKnownTypes) validateReturnTypes(allMethods)
    def getShownFormMethods(fp:FormProcessor) = if (hideUnknownTypes) filterReturnTypes(getFormMethods(fp)) else getFormMethods(fp)
    val methodsInfoNew = fps.map{case (fp, c) => (getShownFormMethods(fp).map{ case (m, _) => (m, c) }, fp.getClassName)}
    methodsInfoNew
  }
  def generate(prefix:String):String = generate(getFormDetails(prefix))
  
  def generateFilteredIn(prefix:String, includeOnly:List[(String, String)])(implicit ignoreMethods:List[(String, String)] = Nil):String = 
    generate(filterMainMethodsInclude(filterMainMethodsExclude(getFormDetails(prefix), ignoreMethods), includeOnly))
  
  def generateFilteredOut(prefix:String, ignoreMethods:List[(String, String)]):String = 
    generate(filterMainMethodsExclude(getFormDetails(prefix), ignoreMethods))
  
  def filterMainMethodsInclude(mainMethods:List[(List[(ScalaMethod, AnyRef)], String)], 
                        includeOnly:List[(String, String)]) = { // includeOnly is list of methods (class/method) name pairs
    mainMethods.collect{
      case methodListClsName => 
        val (methodList, clsName) = methodListClsName 
        val newMethodList = methodList.collect {
          case (sm, a) if includeOnly.exists{ case(methodName, className) => 
                sm.name == methodName && sm.parentClassName.startsWith(className)
            } =>  (sm, a)
        }        
        (newMethodList, clsName)
    }.collect {
      case (methodList, clsName) if methodList.size > 0 => (methodList, clsName) 
    }
  }
  def filterMainMethodsExclude(
    mainMethods:List[(List[(ScalaMethod, AnyRef)], String)], 
    ignoreMethods:List[(String, String)]
  ) = { // includeOnly is list of methods (class/method) name pairs
    mainMethods.collect{
      case methodListClsName => 
        val (methodList, clsName) = methodListClsName 
        val newMethodList = methodList.collect {
          case (sm, a) if !ignoreMethods.exists{ 
              case(methodName, className) => 
                sm.origName == methodName && sm.parentClassName.startsWith(className)              
            } =>  
              (sm, a)
        }        
        (newMethodList, clsName)
    }.collect {
      case (methodList, clsName) if methodList.size > 0 => (methodList, clsName) 
    }
  }
  def generate(mainMethods:List[(List[(ScalaMethod, AnyRef)], String)]):String = { // last param is class name
    getPage(mainMethods, appInfo, postUrl, this, pageTitle)
  }
}








package org.sh.reflect
import org.sh.reflect.{FormProcessor => FP}
import org.sh.reflect.{TypeHandler => TH}
//import org.sh.reflect.{DefaultTypeHandler => TH}
import org.sh.reflect.Util._
import org.sh.reflect.DataStructures._
import org.sh.reflect.CodeGenUtil._

/**
 * purpose of this class
 * 
 * The idea of a proxy is as follows:
 * 
 * [Java class (A)] <---> [EasyProxy (P)] <---- <network, etc via QueryMaker> ---->  [Client (C)]
 * 
 * The tool takes in the Java class A and allows a client to communicate via the proxy P to make queries to A
 * 
 * this class reads in the code of A and automatically generates the code for client C given some QueryMaker 
 * A QueryMaker is essentially a method to query the proxy (via HTTP/XML/Socket/etc)
 * 
 */
class ClientCodeGenerator(cqm: QueryMaker) {
  import CodeGenUtil._
  
  def autoGenerateToFile(newClassName:String, pkg:String, classToProcess:AnyRef):Unit = 
    autoGenerateToFile(newClassName:String, pkg:String, classToProcess:AnyRef, defaultPrefix)
  def autoGenerateToFile(newClassName:String, pkg:String, classToProcess:AnyRef, tag:String):Unit = {
    val className = newClassName+"AutoGen"
    val file = "src/"+pkg.replace(".", "/")+"/"+className+".scala"
    println("Writing to file: "+file)
    org.sh.utils.common.file.Util.writeToTextFile(file, generate(className, getDefaultPid(classToProcess), new FP(tag, classToProcess, new TH), pkg))
  } 
  def generate(className:String, processorID:String, fp:FormProcessor, pkg:String):String = {
    val mainMethods = getFormMethods(fp).sortWith((x, y) => x._1.name < y._1.name)
    val th = fp.getTypeHandler
    val metaMethods = getFormMethods(new FormProcessor(defaultPrefix, fp, th))
    val allMethods = mainMethods ::: metaMethods
    validateReturnTypes(allMethods)
    validateParamTypes(allMethods)
    val newTypes = th.getClass.getCanonicalName :: "org.sh.utils.common.json.JSONUtil" :: cqm.getClass.getCanonicalName :: getNewTypes(allMethods)
    val preString = "/*\n"+preamble(this)+"*/\n"+ newTypes.foldLeft("package "+pkg+"\n\n")((x, y)=> x + "import "+y+"\n")+"\n"
    val initString =
      "  private def split(a:Array[(String, Any)]):(Array[String], Array[Object]) = (a.map(_._1), a.map(_._2.asInstanceOf[AnyRef]))\n"+
      "  private val th = new "+th.getClass.getSimpleName+"\n"+
      "  private def extractResp[T](t:Class[T], s:String) = th.stringToType(t, s).asInstanceOf[T]"
    def substNew(st:String) = subst(newTypes.map(x => (x, toSimpleType(x))).foldLeft(st)((x, y) => x.replace(y._1, y._2)))
    def extr(s:String, t:Class[_]) = "    extractResp(classOf["+substNew(t.getCanonicalName)+"], "+s+")\n"
    def methodBody(methods:List[(ScalaMethod, Class[_])], pid:String) =
      methods.foldLeft("  ")((x, y)=> x + "\n  "+substNew(y._1.toScalaString) + " = {\n"+ body(y._1) + end+ extr(ret(y._1, y._2, pid), y._2) + "  }  ")
    
    val methodsString = methodBody(mainMethods, processorID)+methodBody(metaMethods, EasyProxy.metaPid(processorID))
    preString+"class "+className+"(qm: "+cqm.getClass.getSimpleName+") {\n"+ initString+"\n"+methodsString +"\n}"
  }
  def getNewTypes(methods:List[(ScalaMethod, Class[_])]) =
    methods.map(m => subst(m._2.getCanonicalName) :: m._1.params.map(x => subst(x.paraType.getClassName))).flatten.distinct.filter(_.contains("."))
    //methods.map(m => substExact(m._2.getCanonicalName) :: m._1.params.map(x => substExact(x.paraType.getClassName))).flatten.distinct.filter(_.contains("."))
    //  def substExact(st:String) = javaTypes.find(_._1 == st) match {
    //    case Some((a, b)) => b
    //    case _ => throw new Exception("Type not supported: "+st)
    //  }
  def getFormMethods(fp:FormProcessor) = fp.getPublicMethods.zip(fp.getPublicMethodsTypes.map(_._2))
  def toSimpleType(s:String) = s.split('.').last

  def paramBody(p:Param)  = "    kv = kv :+ (\""+p.paraName+"\", "+p.paraName+")\n"
  def body(m:ScalaMethod) = m.params.foldLeft("    var kv:Array[(String, Any)] = Array()\n")((x, y) =>x+ paramBody(y))
  def end = "    val a = split(kv)\n"
  def ret(m:ScalaMethod, c:Class[_], pid:String) = "qm.makeQuery(\""+pid+"\", \""+m.name+"\", JSONUtil.createJSONString(a._1, a._2))"
}

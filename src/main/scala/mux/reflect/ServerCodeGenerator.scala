
package mux.reflect

import Util._
import mux.reflect.DataStructures._ 

/**
 * purpose of this class
 * 
 * The idea of a proxy is as follows:
 * 
 * [Java classes (A1, A2, ...)] <---> [Proxy (P)] <---- <network, etc via QueryMaker> ---->  [Client (C)]
 * 
 * The tool takes in the Java classes A1, A2, ... and allows a client to communicate via the proxy P to make queries to them
 * 
 * This class takes in the classes A1, A2, ... and outputs a single class A that "wraps" the methods of those classes into one class
 * The wrapper methods also "sanitize" the output types of the original classes:
 *   In case the classes A1, A2, ... have any methods that return Seq or some other collection of any type, this converts them to an Array[String] type
 *   Similarly any unrecognized type is converted to a String type.
 *   
 *   After this, the generated class A can be used as follows:
 *   
 * [Java class (A)] <---> [Proxy (P)] <---- <network, etc via QueryMaker> ---->  [Client (C)]
 * 
 * where A wraps the functionality of A1, A2, ...
 * 
 */

class ServerCodeGenerator(c: List[AnyRef]) {
  import CodeGenUtil._
  val fps = c map (new FormProcessor("", _, DefaultTypeHandler))
  def autoGenerateToFile(objName:String, pkg:String) = {
    val objectName = objName+"AutoGen"
    val file = "src/"+pkg.replace(".", "/")+"/"+objectName+".scala"
    println("Writing to file: "+file)
    org.sh.utils.common.file.Util.writeToTextFile(file, generate(objectName, pkg))
  } 
  def generate(objName:String, pkg:String, prefix:String=defaultPrefix):String = {
    assert(prefix != "", "prefix cannot be empty.")
    val mainMethods = fps.flatMap(getFormMethods(_))
    // val mainMethods = fps.flatMap(getFormMethods(_).sortWith((x, y) => x._1.name < y._1.name))
    validateParamTypes(mainMethods)
    val newTypes =  c.map(_.getClass.getName).map(x => if (x.endsWith("$")) x.init else x)
    val preString = "/*\n"+preamble(this)+"*/\n"+ "package "+pkg+"\n\n"
    // val initString = "  private def arr(a:Array[_]) = a map (_.toString)"
    val initString = "  def arr(s:Seq[Any]) = s.map(_.toString).toArray"
    def body(sm:ScalaMethod) = {
      val retTypeName = sm.returnType.getClassName
      val isSeq = retTypeName.endsWith("[]") || retTypeName == "scala.collection.immutable.List"
      val isOkType = exactMatch(retTypeName) || isSeq
      val str = subst(sm.toScalaSignatureString)
      "/* original return type: "+retTypeName+"*/\n"+
      "  def "+prefix+str+ " = "+ (if(isSeq) "arr(" else "") + str+ 
                                (if(isOkType) "" else ".toString") + 
                                (if(isSeq) ")" else "")
    }
    def methodBody(methods:List[(ScalaMethod, Class[_])]) = methods.foldLeft("  ")((x, y)=> x + "\n  "+body(y._1))
    
    val methodsString = methodBody(mainMethods)
    val importString = newTypes.foldLeft("\n")((x, y)=> x + "  import "+y+"._\n")+"\n"
    preString+"object "+objName+" {\n"+ importString+initString+"\n"+methodsString +"\n}"
  }
}

package org.sh.reflect

import java.io._
import java.lang.reflect.Method

import org.objectweb.asm.{ClassReader, Type}
import org.objectweb.asm.tree.{AbstractInsnNode, ClassNode, LocalVariableNode, MethodNode}
import org.objectweb.asm.util.{Textifier, TraceMethodVisitor}
import org.sh.reflect.DataStructures._
import org.sh.utils.file._

import scala.jdk.CollectionConverters._

object Util extends TraitFilePropertyReader {
  sealed trait JavaVer
  object Java8 extends JavaVer
  object Java7 extends JavaVer
  object Java11 extends JavaVer
  private val javaPrefix = System.getProperty("java.runtime.version").substring(0, 3)
  val javaVer:JavaVer = javaPrefix match {
    case "1.8" => Java8
    case "1.7" => Java7
    case "11." => Java11
    case _ => throw new Exception(s"Unsupported java version $javaPrefix")
  }

  val propertyFile = "reflect.properties"
  var debug = read("debug", false)
  val defaultPrefix = "_" // Methods starting with this will be considered in the EasyProxy
  val defaultMetaPrefix = "____" // Meta methods start with this. Meta methods are methods about methods (i.e., giving info about other methods)
  def getDefaultPid(cls:AnyRef) = {
    // Important. We can only handle ONE instance of a class at a time.
    // to handle multiple instances, use object Foo extends Bar(...)
    // Why? We don't know how to match pid and class instances. Since instances are runtime, while pids (for html etc) are generated at compile-time
    org.sh.utils.Util.sha256Small(org.sh.utils.Util.sha256Small(cls.getClass.getCanonicalName)+"iowy8uhciergf674tgycb4yt4gb") collect {
      case c if "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".contains(c.toUpper) => c
    }
  }
  def NOTImplementedOut[T<: AnyRef](t:T):String = throw new Exception(t.getClass.getName+": Output type not implemented")
  def NOTImplementedIn[T<: AnyRef](s:String):T = throw new Exception(s+": Input type not implemented")

  /**
   * extracts the names, parameter names and parameter types of all methods of declaringClass that start with startString
   * stackoverflow.com/questions/7640617/how-to-get-parameter-names-and-types-via-reflection-in-scala-java-methods
   */
  case class MethodVar(name:String, desc:String) {
    override def toString: String = s"$name: $desc"
  }

  def getMethods(c:AnyRef, is:java.io.InputStream) = {
    val cn = new ClassNode();
    val cr = new ClassReader(is);
    cr.accept(cn, 0);
    is.close();
    //@SuppressWarnings("unchecked")
    val methods = cn.methods.asInstanceOf[java.util.List[MethodNode]];
    var mList:List[ScalaMethod] = Nil
    if (methods.size > 0) for (i <- 1 to methods.size) {
      val m:MethodNode = methods.get(i-1)
      try {
        val argTypes:Array[Type] = Type.getArgumentTypes(m.desc);
        val vars = m.localVariables.asInstanceOf[java.util.List[LocalVariableNode]];

        var paramList:List[Param] = Nil
        if (argTypes.length > 0 && vars.asScala.length > 0) {
          val methodVars = vars.asScala.map{ v =>
            v.index -> MethodVar(v.name, v.desc)
          }.sortBy(_._1)
          val methodVarIt = methodVars.toIterator
          methodVarIt.next // first one refers to 'this'
          for (i <- 1 to argTypes.length) {
            val param = Param(methodVarIt.next._2.name, argTypes(i-1))
            paramList = param :: paramList
          }
        }
        // refer http://stackoverflow.com/a/19173813/243233
        // http://stackoverflow.com/questions/31367070/how-to-read-a-final-string-value-in-asm/31367264#31367264
        val infoVars:Option[LocalVariableNode] =
          vars.asScala.find(_.name == "$INFO$").find(
            info => Type.getType(info.desc) == Type.getType(classOf[String])
          )

        // val with name "$INFO$" and type String gives info about method
        // group seems to be unused
        val groupVars = vars.asScala.find(_.name == "$group$").find(groupDesc => Type.getType(groupDesc.desc) == Type.getType(classOf[String])) // var names "$group$" gives info about which group method belongs

        val allVars = vars.asScala.filter(x => x.name.startsWith("$") && x.name.endsWith("$")).filter{v =>
          Type.getType(v.desc) == Type.getType(classOf[String])
        }.toArray
        
        def insnToString(insn:AbstractInsnNode) = {
          val printer = new Textifier();
          val mp = new TraceMethodVisitor(printer); 
          insn.accept(mp);
          val sw = new StringWriter();
          printer.print(new PrintWriter(sw));
          printer.getText().clear();
          sw.toString();          
        }
        def getStr(optVarNode:Option[LocalVariableNode]) = {

          optVarNode.map {varNode => 
           try {
             val textNode = varNode.start.getPrevious
             /* other combinations to try:
               varNode.start
               varNode.start.getPrevious // this one works currently (Java 8/Scala 2.12)
               varNode.start.getPrevious.getPrevious // this one worked previously (Java 7/Scala 2.11) but no longer
               varNode.start.getPrevious.getPrevious.getPrevious
               varNode.end
              */
             //val text = insnToString(varNode.start.getPrevious.getPrevious).trim // earlier Java/Scala version
             val text = insnToString(textNode).trim
             if (text.startsWith("LDC")) text.substring(5).init else "none"
           } catch {
             case e:Throwable =>
               println(" [Reflect] Error retrieving info")
               e.printStackTrace()
               "Error retrieving info"
           }
         }          
        }
        val infoStr = getStr(infoVars)
        val groupStr = getStr(groupVars)

        val allStr = allVars.map(allVar => allVar.name -> getStr(Some(allVar))).collect{
          case (name, Some(value)) => name -> value
        }.toMap

        val paramVars = paramList.map{p =>
          vars.asScala.find(_.name == "$"+p.paraName+"$").find(info => Type.getType(info.desc) == Type.getType(classOf[String]))
        }.map(getStr)
        (paramList zip paramVars) foreach{case (p, optInfo) =>
          p.info = optInfo
        }
        val sm = ScalaMethod(
          m.name, Type.getReturnType(m.desc), paramList.reverse, c.getClass.getCanonicalName, m.name, infoStr, groupStr, allStr
        )
        mList = sm :: mList
      } catch { 
        case e:Throwable =>
          println (s" [ERROR] Method [${m.name}]. Error message: "+e.getMessage)
          e.printStackTrace()
        }
    }
    mList.reverse
  }
  import scala.reflect.runtime.universe._
  def getObjectInstance(clsName: String):AnyRef = {
    val mirror = runtimeMirror(getClass.getClassLoader)
    val module = mirror.staticModule(clsName)
    mirror.reflectModule(module).instance.asInstanceOf[AnyRef]
  }

  
  def getMethods(c:Class[_], processSuperClass:Boolean):List[ScalaMethod] = {   
    val t = Type.getType(c);
    val url = t.getInternalName() + ".class";
    val is = try {
      val cl = c.getClassLoader();
      val is = cl.getResourceAsStream(url);
      if (is == null) throw new IllegalArgumentException("Cannot find class: " + url);
      else is
    } catch {
      case e:IllegalArgumentException => 
        println (" [IllegalArgumentException] "+Thread.currentThread().getContextClassLoader().getResource(url))
        new java.io.FileInputStream(url)
    }
    val thisClassMethods = getMethods(c, is)
    val thisClassMethodsSig = thisClassMethods.map(_.canonicalSignature)
    
    thisClassMethods ++  {            
      if (processSuperClass) {
        val superClass = c.getSuperclass
        (
            superClass.getName match {
            case "java.lang.Object" => Nil
            case _ => 
              getMethods(superClass, processSuperClass:Boolean).filterNot{m => 
                thisClassMethodsSig.contains(m.canonicalSignature) // do not consider overridden methods
              }
          }
        )
      } else Nil
    }
  }
  def getMethods(a:AnyRef, processSuperClass:Boolean):List[ScalaMethod] = 
    getMethods(a.getClass, processSuperClass:Boolean)
  
  def printMethod(m:ScalaMethod) = {
    println (m.name+" => "+m.returnType.getClassName)
    m.params.foreach(p =>
      println ("   "+ p.paraName+":"+p.paraType.getClassName)
    )
  }
  def getJavaMethod(c:AnyRef, m:ScalaMethod):Option[Method] = {
    val x = c.getClass.getMethods.find(
      x => x.getName == m.name && paramsMatch(m.params, x.getParameterTypes)
    )
    x
  }
  def paramsMatch(p1:List[Param], p2:Array[Class[_]]) = {
    p1.size == p2.size && p1.zip(p2).forall(x => paramMatch(x._1, x._2))
  }
  def paramMatch(p1:Param, p2:Class[_]) = {
    p1.paraType.getClassName == p2.getCanonicalName
  }
}



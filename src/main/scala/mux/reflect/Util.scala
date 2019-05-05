package mux.reflect

import java.io._
import java.lang.reflect.Method
import org.objectweb.asm.util.Textifier
import org.objectweb.asm.util.TraceMethodVisitor
import scala.collection.JavaConversions._
import mux.reflect.DataStructures._
import org.objectweb.asm.ClassReader
import org.objectweb.asm.Type
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.LocalVariableNode
import org.objectweb.asm.tree.MethodNode
import org.sh.utils.common.encoding.Base64
import org.sh.utils.common.file._

//object Util extends TraitPlaintextFileProperties {
object Util extends TraitFilePropertyReader {
  val propertyFile = "reflect.properties"
  var debug = read("debug", false)
  val defaultPrefix = "_" // Methods starting with this will be considered in the Proxy
  val defaultMetaPrefix = "____" // Meta methods start with this. Meta methods are methods about methods (i.e., giving info about other methods)
  def getDefaultPid(cls:AnyRef) = {
    // Important. We can only handle ONE instance of a class at a time.
    // to handle multiple instances, use object Foo extends Bar(...)
    // Why? We don't know how to match pid and class instances. Since instances are runtime, while pids (for html etc) are generated at compile-time
    org.sh.utils.common.Util.sha256Small(org.sh.utils.common.Util.sha256Small(cls.getClass.getCanonicalName)+"iowy8uhciergf674tgycb4yt4gb") collect {
      case c if "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".contains(c.toUpper) => c
    }
  }
  def NOTImplementedOut[T<: AnyRef](t:T):String = throw new Exception(t.getClass.getName+": Output type not implemented")
  def NOTImplementedIn[T<: AnyRef](s:String):T = throw new Exception(s+": Input type not implemented")

  /**
   * extracts the names, parameter names and parameter types of all methods of declaringClass that start with startString
   * stackoverflow.com/questions/7640617/how-to-get-parameter-names-and-types-via-reflection-in-scala-java-methods
   */
  def getMethods(c:AnyRef, is:java.io.InputStream) = {
    val cn = new ClassNode();
    val cr = new ClassReader(is);
    cr.accept(cn, 0);
    is.close();
    //@SuppressWarnings("unchecked")
    val methods = cn.methods.asInstanceOf[java.util.List[MethodNode]];
    var mList:List[ScalaMethod] = Nil
    if (methods.size > 0) for (i <- 1 to methods.size) {
      try {
        val m:MethodNode = methods.get(i-1)
        
        val argTypes:Array[Type] = Type.getArgumentTypes(m.desc);
        val vars = m.localVariables.asInstanceOf[java.util.List[LocalVariableNode]];
        var paramList:List[Param] = Nil
        if (argTypes.length > 0 && vars.length > 0) for (i <- 1 to argTypes.length) {
          // The first local variable actually represents the "this" object in some cases
          val difference = if (vars.get(0).name == "this") 0 else 1
          paramList = Param(vars.get(i-difference).name, argTypes(i-1)) :: paramList
        }

        
        /////////////////////////////////////////////////////////////////////////////////        
        // refer http://stackoverflow.com/a/19173813/243233
        // http://stackoverflow.com/questions/31367070/how-to-read-a-final-string-value-in-asm/31367264#31367264
        val infoVars = vars.find(_.name == "$info$").find(info => Type.getType(info.desc) == Type.getType(classOf[String])) // var names "$info$" gives info about method
        val groupVars = vars.find(_.name == "$group$").find(groupDesc => Type.getType(groupDesc.desc) == Type.getType(classOf[String])) // var names "$group$" gives info about which group method belongs

        val allVars = vars.filter(x => x.name.startsWith("$") && x.name.endsWith("$")).filter{v =>
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
             val text = insnToString(varNode.start.getPrevious.getPrevious).trim
             if (text.startsWith("LDC")) text.substring(5).init else "no info"
           } catch {
             case e:Throwable => "error retrieving info"
           }
         }          
        }
        val infoStr = getStr(infoVars)
        val groupStr = getStr(groupVars)
        
        val allStr = allVars.map(allVar => allVar.name -> getStr(Some(allVar))).collect{
          case (name, Some(value)) => name -> value
        }.toMap

        val paramVars = paramList.map{p =>
          vars.find(_.name == "$"+p.paraName+"$").find(info => Type.getType(info.desc) == Type.getType(classOf[String]))
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
          println (" [ERROR] "+e.getMessage)
          if (debug) {
            val m = methods.get(i-1)
            println (" [info] body not found for method: ["+m.name+"] in class: "+cn.name)
            //e.printStackTrace
          }
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
        println (" [ERROR] "+Thread.currentThread().getContextClassLoader().getResource(url))
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



package org.sh.reflect

import org.objectweb.asm._;
import org.objectweb.asm.commons.GeneratorAdapter;

object TransformTest {
  def transform(classReader:ClassReader) = {
    val cw = new ClassWriter(classReader, ClassWriter.COMPUTE_FRAMES|ClassWriter.COMPUTE_MAXS);
    classReader.accept(new TransformerTest(cw), ClassReader.EXPAND_FRAMES);
  }
}
class TransformerTest(cv:ClassVisitor) extends ClassVisitor(Opcodes.ASM5, cv) {
  // http://stackoverflow.com/a/25568283/243233
  //  def transform(b:Array[Byte]):Array[Byte]={
  //    val classReader = new ClassReader(b);
  //    val cw = new ClassWriter(classReader, ClassWriter.COMPUTE_FRAMES|ClassWriter.COMPUTE_MAXS);
  //    classReader.accept(new Test(cw), ClassReader.EXPAND_FRAMES);
  //    return cw.toByteArray();
  //  }

  override
  def visitMethod(access:Int, name:String, desc:String,signature:String, exceptions:Array[String]):MethodVisitor= {

    var v=super.visitMethod(access, name, desc, signature, exceptions);
    if(name.equals("main") && desc.equals("([Ljava/lang/String;)V")) {
      println("MAIN ================> "+name+":"+signature)

      v=new MainTransformer(v, access, name, desc, signature, exceptions);
    }
    return v;
  }
  override
  def visitEnd {
    appendShowTwo;
    super.visitEnd;
  }
  def appendShowTwo {
    val defVisitor = super.visitMethod(Opcodes.ACC_PUBLIC, "showTwo", "()V", null, null);
    defVisitor.visitCode();
    defVisitor.visitFieldInsn(Opcodes.GETSTATIC,
      "java/lang/System", "out", "Ljava/io/PrintStream;");
    defVisitor.visitLdcInsn("Show Two Method");
    defVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL,
      "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false);
    defVisitor.visitInsn(Opcodes.RETURN);
    defVisitor.visitMaxs(0, 0);
    defVisitor.visitEnd();
  }
  class MainTransformer(delegate:MethodVisitor, access:Int, name:String, desc:String,
        signature:String, exceptions:Array[String]) extends GeneratorAdapter(Opcodes.ASM5, delegate, access, name, desc)
  {
    override
    def visitInsn(opcode:Int) {
      if(opcode==Opcodes.RETURN) {
        // before return insert c.showTwo();
        super.visitVarInsn(Opcodes.ALOAD, 1); // variable c
        super.visitMethodInsn(Opcodes.INVOKEVIRTUAL,
            "ClassName", "showTwo", "()V", false);
      }
      super.visitInsn(opcode);
    }
  }
}

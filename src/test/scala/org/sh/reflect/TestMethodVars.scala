package org.sh.reflect

object TestMethodVars extends App {
  println("JAVA " + System.getProperty("java.runtime.version"))

  val fp = new FormProcessor("", TestObj, DefaultTypeHandler, None, false)
  val m = fp.getPublicMethods(0)
  m.infoVars foreach println
  println("Info vars printed")
  assert(m.methodInfo.getOrElse("no info") == "method_info")
  println("INFO VAR passed")

  val cExpected = """{"jsonKey":"jsonVal"}"""
  val a = m.infoVars.get("$a$").getOrElse("noInfo")
  val b = m.infoVars.get("$b$").getOrElse("noInfo")
  val c = m.infoVars.get("$c$").getOrElse("noInfo")

  assert(a == "1", s"Expected 1 found $a")
  assert(b == "hello", s"Expected hello found $b")
  assert(c == cExpected, s"Expected $cExpected found $c")
  println("PARA VARS passed")

}

object TestObj {
  def method(a: Int, b: String, c: String) = {
    val $INFO$ = "method_info"
    val $a$ = "1"
    val $b$ = "hello"
    val $c$ = """{"jsonKey":"jsonVal"}"""
    println("Hello")
  }
}

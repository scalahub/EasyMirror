# EasyMirror
EasyMirror is a set of reflection utilities for use in Scala (and Java). It consists of three components: 
### Form Processor
At a high level, it reads the bytecode of a class with the given name and does the following:
- Find all public methods in the class
- Find the parameters (the names and types) for each method
- Find the return types of each method

### Proxy
It takes the class name, method name and the named parameters and invokes the given method in the class. It then outputs the value returned by the method

### Code Generator 
This contains utilities for auto-generating code in various languages. It uses Form processor internally.

# Usage
Some examples of usage are given in the [test](https://github.com/scalahub/EasyMirror/blob/master/src/test/scala/org/sh/reflect/test "test") folder.
The following code snippet explains usage of the library.

To read the methods in a class (taken from [ProxyTest.scala](https://github.com/scalahub/EasyMirror/blob/master/src/test/scala/org/sh/reflect/test/ProxyTest.scala "ProxyTest.scala")):

```scala
object MyObject {
  def myMethod(a:Int, b:String) = BigInt(a)
  def someOtherMethod(a:Int, b:String) = BigInt(a)
}
val processSuperClass = false

// add the object to the form processor
Proxy.addProcessor("myObjectID", "my", MyObject, DefaultTypeHandler, processSuperClass)
// process only methods starting with "my"
// use the id "myObjectID" to refer to the object when using Proxy

// invoke the methods using the Proxy
Proxy.getResponse("myObjectID", "myMethod", "{'a':1,'b':'hello',c:}")

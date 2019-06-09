# EasyMirror

[![Build Status](https://travis-ci.org/scalahub/EasyMirror.svg?branch=master)](https://travis-ci.org/scalahub/EasyMirror)

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
Some examples of usage are given in the [test](https://github.com/scalahub/EasyMirror/tree/master/src/test/scala/org/sh/reflect "test") folder.
 
The following code snippet explains usage of the library.

To read the methods in a class (motivated from [ProxyTest.scala](https://github.com/scalahub/EasyMirror/blob/master/src/test/scala/org/sh/reflect/ProxyTest.scala "ProxyTest.scala")):

```scala
object MyObject {
  def my_mainMethod(a:Int, b:String) = BigInt(a)
  def my_otherMethod(myParam:String) = {} // returns Unit
  def someOtherMethod(a:Int, b:String) = BigInt(a)
}
val processSuperClass = false
// add the object to the form processor
EasyProxy.addProcessor("myObjectID", "my_", MyObject, DefaultTypeHandler, processSuperClass)
// process only methods starting with "my"
// use the id "myObjectID" to refer to the object when using Proxy

// invoke the my_mainMethod using the Proxy, but skip the prefix ("my_") when calling
println(EasyProxy.getResponse("myObjectID", "mainMethod", "{'a':'1','b':'hello'}"))
// prints 1

// following will also work because method name in original object starts with "my_"
println(EasyProxy.getResponse("myObjectID", "otherMethod","{'myParam':'hello'}"))
// prints Ok, the string returned for Unit 

// get details for methods available for invocation; append "Meta" to object id to get meta object id.
// meta object is the object that stores information about the main object

println(EasyProxy.getResponse("myObjectIDMeta", "getMethodsInScala", ""))
// prints:
// ["def otherMethod(myParam:String): void","def mainMethod(a:int, b:String): scala.math.BigInt"]
```

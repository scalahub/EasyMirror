name := "CommonReflect"

version := "0.1"

scalaVersion := "2.12.8"

lazy val CommonUtil = RootProject(uri("https://github.com/scalahub/CommonUtil.git"))
// lazy val CommonUtil = RootProject(uri("../CommonUtil"))

lazy val root = project in file(".") dependsOn CommonUtil

libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.5.8"

libraryDependencies += "org.scala-lang.modules" %% "scala-xml" % "1.0.6"

libraryDependencies += "commons-codec" % "commons-codec" % "1.12"

// https://mvnrepository.com/artifact/cglib/cglib
libraryDependencies += "cglib" % "cglib" % "3.2.12"

// https://mvnrepository.com/artifact/org.json/json
libraryDependencies += "org.json" % "json" % "20140107"

// https://mvnrepository.com/artifact/org.bouncycastle/bcprov-jdk15on
libraryDependencies += "org.bouncycastle" % "bcprov-jdk15on" % "1.61"

// https://mvnrepository.com/artifact/org.ow2.asm/asm-all
libraryDependencies += "org.ow2.asm" % "asm-all" % "5.2"

libraryDependencies += "org.scala-lang" % "scala-reflect" % scalaVersion.value

// https://mvnrepository.com/artifact/com.google.jimfs/jimfs
libraryDependencies += "com.google.jimfs" % "jimfs" % "1.1"

// https://mvnrepository.com/artifact/commons-fileupload/commons-fileupload
libraryDependencies += "commons-fileupload" % "commons-fileupload" % "1.4"

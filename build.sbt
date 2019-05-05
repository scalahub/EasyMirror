name := "CommonReflect"

version := "0.1"

scalaVersion := "2.12.8"

lazy val CommonUtilGitRepo = "git:https://github.com/scalahub/CommonUtil.git/#master"

lazy val CommonUtil = RootProject(uri(CommonUtilGitRepo))

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

// https://mvnrepository.com/artifact/org.ow2.asm/asm
//libraryDependencies += "org.ow2.asm" % "asm" % "5.1"
// https://mvnrepository.com/artifact/org.ow2.asm/asm-all
libraryDependencies += "org.ow2.asm" % "asm-all" % "5.2"

// https://mvnrepository.com/artifact/asm/asm
// libraryDependencies += "asm" % "asm" % "3.3.1"
// https://mvnrepository.com/artifact/org.ow2.asm/asm
//libraryDependencies += "org.ow2.asm" % "asm" % "7.1"
//
//// https://mvnrepository.com/artifact/org.ow2.asm/asm-tree
////libraryDependencies += "org.ow2.asm" % "asm-tree" % "5.0.3"
//
//// https://mvnrepository.com/artifact/org.ow2.asm/asm-tree
//libraryDependencies += "org.ow2.asm" % "asm-tree" % "7.1"
//
//// https://mvnrepository.com/artifact/org.ow2.asm/asm-util
//libraryDependencies += "org.ow2.asm" % "asm-util" % "7.1"
//
libraryDependencies += "org.scala-lang" % "scala-reflect" % scalaVersion.value

name := "EasyMirror"

ThisBuild / version := "1.0"

lazy val root = (project in file("."))
  .settings(
    mainClass in (Test, run) := Some("org.sh.reflect.TestDoubleProxyServer")
  )

resolvers += "SonaType" at "https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/"

libraryDependencies += "io.github.scalahub" %% "scalautils" % "1.0"

libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.6.0-M5"

libraryDependencies += "org.scala-lang.modules" %% "scala-xml" % "2.0.0-M1"

libraryDependencies += "commons-codec" % "commons-codec" % "1.12"

libraryDependencies += "cglib" % "cglib" % "3.2.12"

libraryDependencies += "org.json" % "json" % "20140107"

libraryDependencies += "org.bouncycastle" % "bcprov-jdk15on" % "1.61"

libraryDependencies += "org.ow2.asm" % "asm-all" % "5.2"

libraryDependencies += "org.scala-lang" % "scala-reflect" % scalaVersion.value

libraryDependencies += "com.google.jimfs" % "jimfs" % "1.1"

libraryDependencies += "commons-fileupload" % "commons-fileupload" % "1.4"

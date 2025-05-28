lazy val root = (project in file("."))
  .enablePlugins(AssemblyPlugin)
  .settings(
    name := "ISCRacer",
    version := "0.1.0-SNAPSHOT",
    scalaVersion := "2.13.14",
    resolvers += "jitpack" at "https://jitpack.io",
    libraryDependencies ++= Seq(
      "com.typesafe.scala-logging" %% "scala-logging" % "3.9.5",
      "ch.qos.logback" % "logback-classic" % "1.5.18",
      "co.fs2" %% "fs2-core" % "3.11.0",
      "co.fs2" %% "fs2-io" % "3.11.0"
    )
  )
ThisBuild / version := "1.0.0"

ThisBuild / scalaVersion := "2.13.12"

libraryDependencies ++= Seq(
  "de.opal-project" % "framework_2.13" % "5.0.0" withSources() withJavadoc())

libraryDependencies += "com.lihaoyi" %% "ujson" % "3.3.1"


assembly / assemblyMergeStrategy := {
  case PathList("META-INF", xs@_*) => MergeStrategy.discard
  case x => MergeStrategy.first
}

assembly / assemblyOption:= (assembly / assemblyOption).value.copy(includeScala = false)

lazy val root = (project in file("."))
  .settings(
    name := "BA-OPAL-static-Dynamic-TestAdapter"
  )

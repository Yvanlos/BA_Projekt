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
/**
Diese Änderungen wurden in JCG hinzugefügt:

 lazy val jcg_static_dynamic_cg_testAdapter = project.settings(
    commonSettings,
    name := "JCG jcg_static_dynamic_cg_TestAdapter Test Adapter",
    libraryDependencies ++= Seq(
        "de.opal-project" % "framework_2.13" % "5.0.0" withSources() withJavadoc(),
        "com.lihaoyi" %% "ujson" % "3.3.1"
    ),
    assembly / aggregate := false,
    publishArtifact := false
).dependsOn(jcg_testadapter_commons)

 lazy val jcg_evaluation = project.settings(
    commonSettings,
    name := "JCG Evaluation",
    resolvers += "soot snapshot" at "https://soot-build.cs.uni-paderborn.de/nexus/repository/soot-snapshot/",
    resolvers += "soot release" at "https://soot-build.cs.uni-paderborn.de/nexus/repository/soot-release/",
    resolvers += Resolver.mavenLocal,
    libraryDependencies += "de.opal-project" %% "hermes" % "5.0.1-SNAPSHOT",
    publishArtifact := false
).dependsOn(
    jcg_testcases,
    jcg_data_format,
    jcg_annotation_matcher,
    jcg_testadapter_commons,
    jcg_wala_testadapter,
    jcg_soot_testadapter,
    jcg_opal_testadapter,
    jcg_doop_testadapter,
    jcg_js_callgraph_testadapter,
    jcg_jelly_testadapter,
    jcg_tajs_testadapter,
    jcg_code2flow_js_testadapter,
    jcg_code2flow_py_testadapter,
    jcg_pycg_testadapter,
    jcg_pyan_testadapter,
    jcg_jarvis_testadapter,
    jcg_dynamic_testadapter,
    jcg_static_dynamic_cg_testAdapter
)
 */
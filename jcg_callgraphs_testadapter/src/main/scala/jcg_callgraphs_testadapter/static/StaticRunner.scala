package jcg_callgraphs_testadapter.static

import org.opalj.br.analyses.AnalysisApplication
import java.io.File
import java.net.URL

import jcg_callgraphs_testadapter.OpalCallgraphTest

object StaticRunner {
  def run(): Unit = {
    // Pfad zu deinem zu analysierenden Jar (bitte anpassen)
    val targetJarFile = new File("target_program.jar")
    if (!targetJarFile.exists()) {
      println(s"Fehler: ${targetJarFile.getAbsolutePath} existiert nicht!")
      return
    }

    val targetJarURL = targetJarFile.toURI.toURL

    println(s"Starte statische Analyse für: ${targetJarFile.getAbsolutePath}")

    // Analyse starten - OpalCallgraphTest aus deinem Package
    val report = AnalysisApplication.runAnalysis(
      OpalCallgraphTest,
      List(targetJarURL),
      List.empty
    )

    println("Analyse abgeschlossen:")
    println(report.toString)
    println("JSON-Ergebnis wurde in: out/jcg_callgraphs_testadapter/callgraph_output.json geschrieben")
  }
}

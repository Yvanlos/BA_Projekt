package statischeAnalyse

import java.io.File
import java.net.URL

import org.opalj.br.analyses.BasicReport
import org.opalj.log.GlobalLogContext
import org.opalj.br.analyses.Project
import statischeAnalyse.StatischeCallGraphAdapter

object StatischeCallGraphRunner {

  def main(args: Array[String]): Unit = {
    // Jars in a list
    val cpFiles: Seq[File] = new File(".")
      .listFiles()
      .filter(f => f.isFile && f.getName.endsWith(".jar"))
      .toSeq

    //no library needed
    if(cpFiles.isEmpty){
      println("Keine .jar-Dateien gefunden!")
      return
    }

    //Für jede Jar-Datein einzeln Analyse durchführen
    cpFiles.foreach { jar =>
      println(s"==> Starte Analyse für: ${jar.getName}")

      val libcpFiles: Seq[File] = Seq.empty
      // Setup Projekt mit OPAL
      implicit val logContaxt = GlobalLogContext
      val project: Project[URL]= StatischeCallGraphAdapter.setupProject(
        Seq(jar),//nur die Aktuelle Jar wird analysiert
        libcpFiles,
        completelyLoadLibraries = true,
        configuredConfig = com.typesafe.config.ConfigFactory.load()
      )
      //JSON-Dateiname pro JAR festlegen
      val outputFileName = s"out/jcg_callgraphs_testadapter/${jar.getName.stripSuffix(".jar")}_callgraph.json"

      //start of the Analyse für Output-File
      val report: BasicReport=StatischeCallGraphAdapter.analyze(
        project,
        parameters = Seq(s"outputFile=$outputFileName"),
        initProgressManagement = _=> null
      )
      // Report ausgeben
      println("Analyse abgechlossen:")
      println(report.toString)
      println("-------------------------------------------------------------")
    }

  }


}
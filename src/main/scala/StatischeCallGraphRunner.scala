import java.io.File
import java.net.URL

import org.opalj.br.analyses.{BasicReport, Project}
import org.opalj.log.GlobalLogContext

object StatischeCallGraphRunner {

  def main(args: Array[String]): Unit = {
    // Prüfen, ob ein JAR übergeben wurde
    if (args.isEmpty) {
      println("Bitte JAR-Datei als Argument angeben!")
      return
    }

    val jarFile = new File(args(0))
    if (!jarFile.exists() || !jarFile.getName.endsWith(".jar")) {
      println(s"${args(0)} ist keine gültige JAR-Datei!")
      return
    }

    // Optional: weitere Argumente für main()
    val mainArgs = args.drop(1)

    println(s"==> Starte Analyse für: ${jarFile.getName}")

    // Setup Projekt mit OPAL
    implicit val logContext = GlobalLogContext
    val project: Project[URL] = StatischeCallGraphAdapter.setupProject(
      Seq(jarFile), // nur diese JAR analysieren
      Seq.empty, // keine Bibliotheken
      completelyLoadLibraries = true,
      configuredConfig = com.typesafe.config.ConfigFactory.load()
    )

    // JSON-Ausgabe-Datei
    val outputFileName = s"jcg_static_dynamic_cg_testAdapter/out/${jarFile.getName.stripSuffix(".jar")}_static.json"
//jcg_static_dynamic_cg_testAdapter
    // Analyse starten
    val report: BasicReport = StatischeCallGraphAdapter.analyze(
      project,
      parameters = Seq(s"outputFile=$outputFileName") ++ mainArgs, // Main-Args optional weitergeben
      initProgressManagement = _ => null
    )

    println("Analyse abgeschlossen:")
    println(report.toString)
  }

}

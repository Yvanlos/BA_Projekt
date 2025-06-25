package jcg_callgraphs_testadapter

import java.io.{File, PrintWriter}
import java.net.URL

import com.typesafe.config.{Config, ConfigValueFactory}
import org.opalj.br.DeclaredMethod
import org.opalj.br.analyses.{Analysis, AnalysisApplication, BasicReport, ProgressManagement, Project, ReportableAnalysisResult}
import org.opalj.log.LogContext
import org.opalj.tac.cg.RTACallGraphKey
import play.api.libs.json._

import scala.collection.mutable

// Case class für JSON-Element im Stil des JVMTI-Agenten
case class CallGraphEntry(caller: String, callee: String)

// JSON Formatter
object CallGraphEntry {
  implicit val writes: Writes[CallGraphEntry] = Json.writes[CallGraphEntry]
}

object StatischeCallGraphAdapter extends Analysis[URL, BasicReport] with AnalysisApplication {

  override def title: String = "Statische Callgraph Adapter"

  override def setupProject(cpFiles: Iterable[File], libcpFiles: Iterable[File], completelyLoadLibraries: Boolean, configuredConfig: Config)(implicit initialLogContext: LogContext): Project[URL] = {
    val newConfig = configuredConfig.withValue(
      "org.opalj.br.analyses.cg.InitialEntryPointsKey.analysis",
      ConfigValueFactory.fromAnyRef("org.opalj.br.analyses.cg.ApplicationEntryPointsFinder")
    )
    super.setupProject(cpFiles, libcpFiles, completelyLoadLibraries, newConfig)
  }

  override def analyze(project: Project[URL], parameters: Seq[String], initProgressManagement: Int => ProgressManagement): BasicReport = {
    val cg = project.get(RTACallGraphKey)

    // Wir wollen alle "call graph edges" sammeln als caller -> callee Paare
    val entries = mutable.ArrayBuffer[CallGraphEntry]()

    // Iteriere alle Kanten: caller -callee- Methode
    // Die Callgraph-API hat Methoden:
    // cg.callersOf(callee) liefert Iterator[(DeclaredMethod, location, Int)]
    // Wir machen es umgekehrt: Iteriere alle Methoden und alle deren Aufrufer

    // Alle erreichbaren Methoden durchlaufen
    val reachableMethods = cg.reachableMethods()

    // Für jede Methode finden wir ihre Aufrufer (callers) und speichern caller -> callee
    reachableMethods.foreach { ctx =>
      val callee = ctx.method
      val callerTriples = cg.callersOf(callee)
      callerTriples.foreach { triple =>
        val caller = triple._1
        entries += CallGraphEntry(
          caller = s"${caller.declaringClassType.fqn}:${caller.name.toString}",
          callee = s"${callee.declaringClassType.fqn}:${callee.name.toString}"
        )
      }
    }

    // Optional: Wenn es Methoden ohne Aufrufer gibt (z.B. Entry Points), dann "TopLevel"
    val calleesWithCaller = entries.map(_.callee).toSet
    val topLevelMethods = reachableMethods.filterNot(ctx => calleesWithCaller.contains(s"${ctx.method.declaringClassType.fqn}:${ctx.method.name}"))
    topLevelMethods.foreach { ctx =>
      entries += CallGraphEntry(
        caller = "TopLevel",
        callee = s"${ctx.method.declaringClassType.fqn}:${ctx.method.name.toString}"
      )
    }

    // JSON schreiben - Array von {caller, callee}
    val json = Json.prettyPrint(Json.toJson(entries))
    val outFile = new File("out/jcg_callgraphs_testadapter/callgraph_output.json")
    outFile.getParentFile.mkdirs()
    val pw = new PrintWriter(outFile)
    try pw.println(json) finally pw.close()

    println(s"JSON output written to ${outFile.getAbsolutePath}")

    // Für BasicReport können wir noch ein paar Stats ausgeben
    val stats = Seq(
      s"Total edges (caller->callee): ${entries.size}",
      s"Reachable methods: ${reachableMethods.size}"
    )
    val detailed = entries.take(20).map(e => s"Caller: ${e.caller} -> Callee: ${e.callee}") // max 20 als Beispiel

    BasicReport(stats ++ detailed)
  }

  override val analysis: Analysis[URL, ReportableAnalysisResult] = this
}

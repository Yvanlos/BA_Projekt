import java.io.{File, PrintWriter}
import java.net.URL

import com.typesafe.config.{Config, ConfigValueFactory}
import org.opalj.br.analyses.{Analysis, AnalysisApplication, BasicReport, ProgressManagement, Project, ReportableAnalysisResult}
import org.opalj.log.LogContext
import org.opalj.tac.cg.RTACallGraphKey
import play.api.libs.json._

// JSON-Datenstrukturen
case class CallerEntry(methodName: String, className: String)
case class Edge(caller: CallerEntry, callee: CallerEntry)

case class CallGraphResult(
                            affectedClasses: Int,
                            numEdge: Int,
                            reachableMethods: Int,
                            edges: Seq[Edge]
                          )

object CallGraphResult {
  implicit val callerEntryWrites: Writes[CallerEntry] = Json.writes[CallerEntry]
  implicit val edgeWrites: Writes[Edge] = Json.writes[Edge]
  implicit val callGraphResultWrites: Writes[CallGraphResult] = Json.writes[CallGraphResult]
}

object StatischeCallGraphAdapter extends Analysis[URL, BasicReport] with AnalysisApplication {

  override def title: String = "Statische Callgraph Adapter"

  override def setupProject(
                             cpFiles: Iterable[File],
                             libcpFiles: Iterable[File],
                             completelyLoadLibraries: Boolean,
                             configuredConfig: Config
                           )(implicit initialLogContext: LogContext): Project[URL] = {
    val newConfig = configuredConfig.withValue(
      "org.opalj.br.analyses.cg.InitialEntryPointsKey.analysis",
      ConfigValueFactory.fromAnyRef("org.opalj.br.analyses.cg.ApplicationEntryPointsFinder")
    )
    super.setupProject(cpFiles, libcpFiles, completelyLoadLibraries, newConfig)
  }

  override def analyze(
                        project: Project[URL],
                        parameters: Seq[String],
                        initProgressManagement: Int => ProgressManagement
                      ): BasicReport = {
    val cg = project.get(RTACallGraphKey)

    // Parameter für Output-Datei auslesen, Standardpfad als Fallback
    val outputPath = parameters
      .find(_.startsWith("outputFile="))
      .map(_.stripPrefix("outputFile="))
      .getOrElse("out/jcg_callgraphs_testadapter/callgraph_output.json")

    // Alle Edges (Caller -> Callee) sammeln
    val edges = cg.reachableMethods().flatMap { ctx =>
      val caller = ctx.method
      cg.calleesOf(caller).flatMap { case (_, callees) =>
        callees.map { callee =>
          Edge(
            CallerEntry(caller.name.toString, caller.declaringClassType.fqn),
            CallerEntry(callee.method.name.toString, callee.method.declaringClassType.fqn)
          )
        }
      }
    }

    val result = CallGraphResult(
      affectedClasses = cg.reachableMethods().map(_.method.declaringClassType).toSet.size,
      numEdge = cg.numEdges,
      reachableMethods = cg.reachableMethods().size,
      edges = edges.toSeq
    )

    // JSON schreiben
    val json = Json.prettyPrint(Json.toJson(result))
    val outFile = new File(outputPath)
    outFile.getParentFile.mkdirs()
    val pw = new PrintWriter(outFile)
    try pw.println(json) finally pw.close()

    println(s"JSON output written to ${outFile.getAbsolutePath}")

    // Ergebnis als BasicReport zurückgeben
    val stats = Seq(
      s"Affected classes: ${result.affectedClasses}",
      s"CG Num Edges = ${result.numEdge}",
      s"CG Reachable Methods = ${result.reachableMethods}",
      s"Exported Edges = ${result.edges.size}"
    )

    val detailedEdges = result.edges.take(50).map(e =>
      s"${e.caller.className}.${e.caller.methodName} -> ${e.callee.className}.${e.callee.methodName}"
    )

    BasicReport(stats ++ Seq("---- Sample Edges ----") ++ detailedEdges)
  }

  override val analysis: Analysis[URL, ReportableAnalysisResult] = this
}

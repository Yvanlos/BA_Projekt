/*package statischeAnalyse



import java.io.{File, PrintWriter}
import java.net.URL

import com.typesafe.config.{Config, ConfigValueFactory}
import org.opalj.br.DeclaredMethod
import org.opalj.br.analyses.{Analysis, AnalysisApplication, BasicReport, ProgressManagement, Project, ReportableAnalysisResult}
import org.opalj.log.LogContext
import org.opalj.tac.cg.RTACallGraphKey
import play.api.libs.json._

import scala.collection.mutable

// Case classes für JSON-Struktur
case class CallGraphResult(
                            affectedClasses: Int,
                            numEdge: Int,
                            reachableMethods: Int,
                            vulnerableMethods: Int,
                            callers: Seq[CallerEntry],
                            callees: Seq[CallerEntry]
                          )

case class CallerEntry(methodName: String, className: String)

// JSON formatter
object CallGraphResult {
  implicit val callerEntryWrites: Writes[CallerEntry] = Json.writes[CallerEntry]
  implicit val callGraphResultWrites: Writes[CallGraphResult] = Json.writes[CallGraphResult]
}

object StatischeCallGraphAdapter extends Analysis[URL, BasicReport] with AnalysisApplication {

  override def title: String = "Statische Callgraph Adapter"

  override def setupProject(cpFiles: Iterable[File], libcpFiles: Iterable[File], completelyLoadLibraries: Boolean, configuredConfig: Config)
                           (implicit initialLogContext: LogContext): Project[URL] = {
    val newConfig = configuredConfig.withValue(
      "org.opalj.br.analyses.cg.InitialEntryPointsKey.analysis",
      ConfigValueFactory.fromAnyRef("org.opalj.br.analyses.cg.ApplicationEntryPointsFinder")
    )
    super.setupProject(cpFiles, libcpFiles, completelyLoadLibraries, newConfig)
  }

  override def analyze(project: Project[URL], parameters: Seq[String], initProgressManagement: Int => ProgressManagement): BasicReport = {
    val cg = project.get(RTACallGraphKey)

    val methodName = "verifyCall"
    val className = "lrr/Demo"

    val reachableMethods = new mutable.HashSet[DeclaredMethod]()

    def addAllCallers(dm: DeclaredMethod): Unit = {
      if (reachableMethods.contains(dm)) return
      cg.callersOf(dm).foreach { triple =>
        val callerMethod = triple._1
        reachableMethods.add(callerMethod)
      }
      cg.callersOf(dm).foreach { triple =>
        addAllCallers(triple._1)
      }
    }

    val targetMethodOpt = cg.reachableMethods().toList.find { ctx =>
      ctx.method.declaringClassType.fqn == className && ctx.method.name == methodName
    }.map(_.method)

    if (targetMethodOpt.isDefined) {
      addAllCallers(targetMethodOpt.get)
    } else {
      println(s"No vulnerable Method found for: $methodName in class $className")
    }

    val callerEntries = reachableMethods.toSeq.map { m =>
      CallerEntry(m.name.toString, m.declaringClassType.fqn)
    }

    // Callees = direkte Zielmethoden (hier targetMethodOpt)
    val calleeEntries = targetMethodOpt.toSeq.map { m =>
      CallerEntry(m.name.toString, m.declaringClassType.fqn)
    }

    val result = CallGraphResult(
      affectedClasses = reachableMethods.map(_.declaringClassType).toSet.size,
      numEdge = cg.numEdges,
      reachableMethods = cg.reachableMethods().size,
      vulnerableMethods = reachableMethods.size,
      callers = callerEntries,
      callees = calleeEntries
    )

    // JSON-Datei schreiben
    val json = Json.prettyPrint(Json.toJson(result))
    val outFile = new File("out/jcg_callgraphs_testadapter/callgraph_output.json")
    outFile.getParentFile.mkdirs()
    val pw = new PrintWriter(outFile)
    try pw.println(json) finally pw.close()

    println(s"JSON output written to ${outFile.getAbsolutePath}")

    // Auch als BasicReport ausgeben
    val stats = Seq(
      s"Affected classes: ${result.affectedClasses}",
      s"CG Num Edges = ${result.numEdge}",
      s"CG Reachable Methods = ${result.reachableMethods}",
      s"CG Vulnerable Methods = ${result.vulnerableMethods}"
    )
    val detailedCallers = result.callers.map(e => s"Caller: ${e.methodName} in ${e.className}")
    val detailedCallees = result.callees.map(e => s"Callee: ${e.methodName} in ${e.className}")

    BasicReport(stats ++ detailedCallers ++ detailedCallees)
  }

  override val analysis: Analysis[URL, ReportableAnalysisResult] = this
}

*/

package statischeAnalyse

import java.io.{File, PrintWriter}
import java.net.URL

import com.typesafe.config.{Config, ConfigValueFactory}
import org.opalj.br.DeclaredMethod
import org.opalj.br.analyses.{Analysis, AnalysisApplication, BasicReport, ProgressManagement, Project, ReportableAnalysisResult}
import org.opalj.log.LogContext
import org.opalj.tac.cg.RTACallGraphKey
import play.api.libs.json._

import scala.collection.mutable

// JSON-Datenstruktur
case class CallGraphResult(
                            affectedClasses: Int,
                            numEdge: Int,
                            reachableMethods: Int,
                            vulnerableMethods: Int,
                            callers: Seq[CallerEntry],
                            callees: Seq[CallerEntry]
                          )

case class CallerEntry(methodName: String, className: String)

object CallGraphResult {
  implicit val callerEntryWrites: Writes[CallerEntry] = Json.writes[CallerEntry]
  implicit val callGraphResultWrites: Writes[CallGraphResult] = Json.writes[CallGraphResult]
}

object StatischeCallGraphAdapter extends Analysis[URL, BasicReport] with AnalysisApplication {

  override def title: String = "Statische Callgraph Adapter"

  override def setupProject(cpFiles: Iterable[File], libcpFiles: Iterable[File], completelyLoadLibraries: Boolean, configuredConfig: Config)
                           (implicit initialLogContext: LogContext): Project[URL] = {
    val newConfig = configuredConfig.withValue(
      "org.opalj.br.analyses.cg.InitialEntryPointsKey.analysis",
      ConfigValueFactory.fromAnyRef("org.opalj.br.analyses.cg.ApplicationEntryPointsFinder")
    )
    super.setupProject(cpFiles, libcpFiles, completelyLoadLibraries, newConfig)
  }

  override def analyze(project: Project[URL], parameters: Seq[String], initProgressManagement: Int => ProgressManagement): BasicReport = {
    val cg = project.get(RTACallGraphKey)

    // Parameter für Output-Datei auslesen, Standardpfad als Fallback
    val outputPath = parameters
      .find(_.startsWith("outputFile="))
      .map(_.stripPrefix("outputFile="))
      .getOrElse("out/jcg_callgraphs_testadapter/callgraph_output.json")

    val methodName = "verifyCall"
    val className = "lrr/Demo"

    val reachableMethods = new mutable.HashSet[DeclaredMethod]()

    def addAllCallers(dm: DeclaredMethod): Unit = {
      if (reachableMethods.contains(dm)) return
      cg.callersOf(dm).foreach { triple =>
        reachableMethods.add(triple._1)
      }
      cg.callersOf(dm).foreach { triple =>
        addAllCallers(triple._1)
      }
    }

    val targetMethodOpt = cg.reachableMethods().toList.find { ctx =>
      ctx.method.declaringClassType.fqn == className && ctx.method.name == methodName
    }.map(_.method)

    if (targetMethodOpt.isDefined) {
      addAllCallers(targetMethodOpt.get)
    } else {
      println(s"No vulnerable Method found for: $methodName in class $className")
    }

    val callerEntries = reachableMethods.toSeq.map { m =>
      CallerEntry(m.name.toString, m.declaringClassType.fqn)
    }

    val calleeEntries = targetMethodOpt.toSeq.map { m =>
      CallerEntry(m.name.toString, m.declaringClassType.fqn)
    }

    val result = CallGraphResult(
      affectedClasses = reachableMethods.map(_.declaringClassType).toSet.size,
      numEdge = cg.numEdges,
      reachableMethods = cg.reachableMethods().size,
      vulnerableMethods = reachableMethods.size,
      callers = callerEntries,
      callees = calleeEntries
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
      s"CG Vulnerable Methods = ${result.vulnerableMethods}"
    )
    val detailedCallers = result.callers.map(e => s"Caller: ${e.methodName} in ${e.className}")
    val detailedCallees = result.callees.map(e => s"Callee: ${e.methodName} in ${e.className}")

    BasicReport(stats ++ detailedCallers ++ detailedCallees)
  }

  override val analysis: Analysis[URL, ReportableAnalysisResult] = this
}

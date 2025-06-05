import com.typesafe.config.{Config, ConfigValueFactory}
import org.opalj.br.DeclaredMethod
import org.opalj.br.analyses.{Analysis, AnalysisApplication, BasicReport, ProgressManagement, Project, ReportableAnalysisResult}
import org.opalj.log.LogContext
import java.io.File
import java.net.URL

import org.opalj.tac.cg.RTACallGraphKey
//import org.opalj.tac.cg.CHACallGraphKey

import scala.collection.mutable

object OpalCallgraphTest extends Analysis[URL, BasicReport] with AnalysisApplication {

  // Title of this Analysis
  override def title: String = "OPAL Callgraph Demo"

  override def setupProject(cpFiles: Iterable[File], libcpFiles: Iterable[File], completelyLoadLibraries: Boolean, configuredConfig: Config)(implicit initialLogContext: LogContext): Project[URL] = {

    val newConfig = configuredConfig.withValue("org.opalj.br.analyses.cg.InitialEntryPointsKey.analysis",
      ConfigValueFactory.fromAnyRef("org.opalj.br.analyses.cg.ApplicationEntryPointsFinder"))

    super.setupProject(cpFiles, libcpFiles, completelyLoadLibraries, newConfig)
  }

  override def analyze(project: Project[URL], parameters: Seq[String], initProgressManagement: Int => ProgressManagement): BasicReport = {

    // Einen CHA Callgraphen generieren. Andere Optionen:
    // - RTACallGraphKey -> Besser als CHA, etwas langsamer
    //val cg = project.get(CHACallGraphKey)
    val cg = project.get(RTACallGraphKey)

    // Zielmethode: "target"
    //val methodName = "verifyCall"
    //val className = "csr/Demo"

    val methodName = "verifyCall"
    val className = "lrr/Demo"

    //val methodName = "target"
    //val className = "Main/java/CallTarget"

    // Set für betroffene Methoden
    val reachableMethods = new mutable.HashSet[DeclaredMethod]()

    def addAllCallers(dm: DeclaredMethod): Unit = {
      // Verhindern von mehrfachen Einträgen
      if (reachableMethods.contains(dm)) return

      // Füge alle Caller dieser Methode hinzu
      cg.callersOf(dm).foreach { triple =>
        val callerMethod = triple._1
        reachableMethods.add(callerMethod)
      }
m
      // Rekursiv alle Caller von Callern hinzufügen
      cg.callersOf(dm).foreach { triple =>
        addAllCallers(triple._1)
      }
    }

    // Suche nach der Methode "target" und finde alle Caller
    val targetMethodOpt = cg
      .reachableMethods()
      .toList
      .find(ctx =>
        ctx.method.declaringClassType.fqn == className &&
          ctx.method.name == methodName)
      .map(_.method)

    if (targetMethodOpt.isDefined) {
      addAllCallers(targetMethodOpt.get)
    } else {
      println(s"No vulnerable Method found for: $methodName in class $className")
    }

    // Liste der betroffenen Methoden für den Report
    val detailedCallerLines: Seq[String] = reachableMethods.map(m => s"Caller: ${m.name} in ${m.declaringClassType.fqn}").toSeq

    // Anzahl der betroffenen Klassen und Methoden
    val numberOfAffectedMethods = reachableMethods.map(m => m.declaringClassType).toList.distinct.size
    val numberOfTotalMethods = cg.reachableMethods().size

    // Stats für den Report
    val stats = List(
      s"Affected classes: ${numberOfAffectedMethods}",
      s"CG Num Edges = ${cg.numEdges}",
      s"CG Reachable Methods = ${cg.reachableMethods().size}",
      s"CG Vulnerable Methods = ${reachableMethods.size}"
    )

    // Kombinierte Liste mit Stats und detaillierten Methoden
    val msgs = stats ++ detailedCallerLines

    // Finaler Report
    BasicReport(msgs)
  }

  override val analysis: Analysis[URL, ReportableAnalysisResult] = this
}

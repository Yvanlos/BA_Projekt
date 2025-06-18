import com.typesafe.config.{Config, ConfigValueFactory}
import org.opalj.br.DeclaredMethod
import org.opalj.br.analyses.{Analysis, AnalysisApplication, BasicReport, ProgressManagement, Project, ReportableAnalysisResult}
import org.opalj.log.LogContext
import org.opalj.tac.cg.CHACallGraphKey

import java.io.File
import java.net.URL
import scala.collection.mutable

object OpalCallgraphDemo extends Analysis[URL, BasicReport] with AnalysisApplication {

  // Title of this Analysis
  override def title: String = "OPAL Callgraph Demo"

  override def setupProject(cpFiles: Iterable[File], libcpFiles: Iterable[File], completelyLoadLibraries: Boolean, configuredConfig: Config)(implicit initialLogContext: LogContext): Project[URL] = {

    println(s"Building project ${cpFiles.head.toPath.toString}")
    val newConfig = configuredConfig.withValue("org.opalj.br.analyses.cg.InitialEntryPointsKey.analysis",
      ConfigValueFactory.fromAnyRef("org.opalj.br.analyses.cg.LibraryEntryPointsFinder"))

    super.setupProject(cpFiles, libcpFiles, completelyLoadLibraries, newConfig)
  }



  override def analyze(project: Project[URL], parameters: Seq[String], initProgressManagement: Int => ProgressManagement): BasicReport = {

    // Einen CHA Callgraphen generieren. Andere Optionen:
    // - RTACallGraphKey -> Besser als CHA, etwas langsamer
    // - XTACallGraphKey -> Besser als RTA, deutlich langsamer
    // - CTACallgRaphKey -> Bester (von OPAL unterstüzter) Algorithmus
    val cg = project.get(CHACallGraphKey)

    val vulnerableMethodOpt = cg
      .reachableMethods()
      .toList
      .find(ctx => ctx.method.declaringClassType.fqn.equals("java/lang/Object") && ctx.method.name.equals("toString") ).map(_.method)


    val reachableMethods = new mutable.HashSet[DeclaredMethod]()

    def addAllCallers(dm: DeclaredMethod): Unit = {

      if(reachableMethods.contains(dm)) return

      cg.callersOf(dm).foreach { triple =>
        val callerMethod = triple._1

        reachableMethods.add(callerMethod)
      }

      cg.callersOf(dm).foreach{ triple =>
        addAllCallers(triple._1)
      }

    }

    if(vulnerableMethodOpt.isDefined)
      addAllCallers(vulnerableMethodOpt.get)
    else
      println("No vulnerable Method found")


    val numberOfAffectedMethods = reachableMethods.map(m => m.declaringClassType).toList.distinct.size
    val numberOfTotalMethods = cg.reachableMethods().size

    println(s"Affected classses: ${numberOfAffectedMethods / numberOfTotalMethods}%")


    val msgs = List(
      "CG Num Edges = " + cg.numEdges,
      "CG Reachable Methods = " + cg.reachableMethods().size,
      "CG Vulnerable Methods = " + reachableMethods.size
    )

    BasicReport(msgs)
  }
  override val analysis: Analysis[URL, ReportableAnalysisResult] = this
}

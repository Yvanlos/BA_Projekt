import java.io.File
import scala.io.Source
import ujson._

case class ExpectedCG(
                       links: Seq[Seq[String]],
                       indirectLinks: Seq[String] = Seq()
                     )

trait CallGraph {
  def filePath: String
  def links: Seq[Seq[String]]

  /** Vergleich gegen Ground Truth, gibt alle wichtigen Metriken zurück */
  def compareLinks(expectedCG: ExpectedCG): ComparisonResult = {
    var foundEdges: Seq[Seq[String]] = Seq()
    var missingEdges: Seq[Seq[String]] = Seq()

    // Fehlende Edges (FN) und gefundene Edges (TP)
    for (expectedEdge <- expectedCG.links) {
      if (!links.exists(edge => edgesMatch(edge, expectedEdge))) {
        missingEdges :+= expectedEdge
      } else {
        foundEdges :+= expectedEdge
      }
    }

    // Extra Edges (FP)
    val extraEdges = links.filter(edge => !expectedCG.links.exists(exp => edgesMatch(edge, exp)))

    // Precision / Recall
    val recall = if (expectedCG.links.nonEmpty) foundEdges.size.toDouble / expectedCG.links.size else 1.0
    val precision = if (links.nonEmpty) foundEdges.size.toDouble / links.size else 1.0

    ComparisonResult(
      foundEdges,
      missingEdges,
      extraEdges,
      precision,
      recall,
      callGraphSize = links.size,
      groundTruthSize = expectedCG.links.size
    )
  }

  private def normalizeMethodName(name: String): String = {
    // Nur MethodenNamen bis zur ersten '(' behalten
    name.takeWhile(_ != '(')
  }
  /*
    private def edgesMatch(edge: Seq[String], expectedEdge: Seq[String]): Boolean = {
      edge.zip(expectedEdge).forall { case (e, ee) =>
        if (!ee.contains(":")) e.split(":").head == ee else e == ee
      }
    }
  }
  */
  private def edgesMatch(edge: Seq[String], expectedEdge: Seq[String]) : Boolean = {
    edge.zip(expectedEdge).forall { case (e, ee) =>
      val Array(eClass, eMethod) = e.split(":", 2)
      val Array(eeClass, eeMethod) = ee.split(":", 2)
      eClass == eeClass && normalizeMethodName(eMethod) == normalizeMethodName(eeMethod)
    }
  }
}
case class ComparisonResult(
                             foundEdges: Seq[Seq[String]],   // True Positives
                             missingEdges: Seq[Seq[String]], // False Negatives
                             extraEdges: Seq[Seq[String]],   // False Positives
                             precision: Double,
                             recall: Double,
                             callGraphSize: Int,
                             groundTruthSize: Int
                           ) {
  def truePositives: Int = foundEdges.size
  def falsePositives: Int = extraEdges.size
  def falseNegatives: Int = missingEdges.size
}

case class JsonCallGraph(filePath: String, links: Seq[Seq[String]]) extends CallGraph

object CompareCallGraphs {
  def main(args: Array[String]): Unit = {
    if (args.length != 2) {
      println("Usage: runMain CompareCallGraphs <groundTruth.json> <callGraph.json>")
      sys.exit(1)
    }

    val groundTruthFile = new File(args(0))
    val callGraphFile   = new File(args(1))

    // Beide JSON-Dateien bestehen aus Arrays
    val groundJson = ujson.read(Source.fromFile(groundTruthFile).mkString).arr
    val expectedCG = ExpectedCG(
      links = groundJson.map(edge => Seq(
        edge("caller")("className").str + ":" + edge("caller")("methodName").str,
        edge("callee")("className").str + ":" + edge("callee")("methodName").str
      )).toSeq
    )

    val callJson = ujson.read(Source.fromFile(callGraphFile).mkString).arr
    val callGraph = JsonCallGraph(
      filePath = callGraphFile.getAbsolutePath,
      links = callJson.map(edge => Seq(
        edge("caller")("className").str + ":" + edge("caller")("methodName").str,
        edge("callee")("className").str + ":" + edge("callee")("methodName").str
      )).toSeq
    )

    val result = callGraph.compareLinks(expectedCG)

    println(s"Call Graph Größe: ${result.callGraphSize}")
    println(s"Ground Truth Größe: ${result.groundTruthSize}")
    println(s"True Positives (TP): ${result.truePositives}")
    println(s"False Positives (FP): ${result.falsePositives}")
    println(s"False Negatives (FN): ${result.falseNegatives}")
    println(s"Gefundene Edges : ${result.foundEdges.map(_.mkString(" -> ")).mkString(", ")}")
    println(s"Fehlende Edges: ${result.missingEdges.map(_.mkString(" -> ")).mkString(", ")}")
    println(s"Extra Edges: ${result.extraEdges.map(_.mkString(" -> ")).mkString(", ")}")
    println(f"Precision: ${result.precision}%.3f")
    println(f"Recall: ${result.recall}%.3f")
  }
}

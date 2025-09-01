import java.io.File
import scala.util.Using
import ujson.Value

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

    // === Fehlende Edges ermitteln ===
    for (expectedEdge <- expectedCG.links) {
      if (!links.exists(edge => edgesMatch(edge, expectedEdge))) {
        missingEdges :+= expectedEdge
      } else {
        foundEdges :+= expectedEdge
      }
    }

    // === Extra Edges im CallGraph ===
    val extraEdges = links.filter(edge => !expectedCG.links.exists(exp => edgesMatch(edge, exp)))

    // === Metrics ===
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

  private def edgesMatch(edge: Seq[String], expectedEdge: Seq[String]): Boolean = {
    edge.zip(expectedEdge).forall { case (e, ee) =>
      if (!ee.contains(":")) e.split(":").head == ee else e == ee
    }
  }
}

case class ComparisonResult(
                             foundEdges: Seq[Seq[String]],
                             missingEdges: Seq[Seq[String]],
                             extraEdges: Seq[Seq[String]],
                             precision: Double,
                             recall: Double,
                             callGraphSize: Int,
                             groundTruthSize: Int
                           )

case class JsonCallGraph(filePath: String, links: Seq[Seq[String]]) extends CallGraph

object CompareCallGraphs {
  def main(args: Array[String]): Unit = {
    if (args.length != 2) {
      println("Usage: runMain CompareCallGraphs <groundTruth.json> <callGraph.json>")
      sys.exit(1)
    }

    val groundTruthFile = new File(args(0))
    val callGraphFile   = new File(args(1))

    val groundJson = ujson.read(scala.io.Source.fromFile(groundTruthFile).mkString)
    val expectedCG = ExpectedCG(
      links = groundJson.arr.map(edge => Seq(
        edge("caller")("className").str,
        edge("callee")("className").str
      )).toSeq
    )

    val callJson = ujson.read(scala.io.Source.fromFile(callGraphFile).mkString)
    val callGraph = JsonCallGraph(
      filePath = callGraphFile.getAbsolutePath,
      links = callJson.arr.map(edge => Seq(
        edge("caller")("className").str,
        edge("callee")("className").str
      )).toSeq
    )

    val result = callGraph.compareLinks(expectedCG)

    println(s"Call Graph Größe: ${result.callGraphSize}")
    println(s"Ground Truth Größe: ${result.groundTruthSize}")
    println(s"Gefundene Edges in CallGraph: ${result.foundEdges.map(_.mkString(" -> ")).mkString(", ")}")
    println(s"Fehlende Edges: ${result.missingEdges.map(_.mkString(" -> ")).mkString(", ")}")
    println(s"Extra Edges: ${result.extraEdges.map(_.mkString(" -> ")).mkString(", ")}")
    println(f"Precision: ${result.precision}%.3f")
    println(f"Recall: ${result.recall}%.3f")
  }
}

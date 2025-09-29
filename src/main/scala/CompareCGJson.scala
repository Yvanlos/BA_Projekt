import java.io.File
import scala.io.Source
import ujson._
import scala.collection.mutable

case class ExpectedCG(
                       links: Seq[Seq[String]]
                     )

case class ComparisonResult(
                             truePositives: Int,
                             falsePositives: Int,
                             falseNegatives: Int,
                             precision: Double,
                             recall: Double,
                             f1: Double,
                             foundEdges: Seq[Seq[String]],
                             missingEdges: Seq[Seq[String]],
                             extraEdges: Seq[Seq[String]]
                           )

case class JsonCallGraph(filePath: String, links: Seq[Seq[String]])

object CompareCGJson {

  def main(args: Array[String]): Unit = {
    var cg1Path = ""
    var cg2Path = ""
    var showPrecisionRecall = false
    var showAdditional = false
    var showCommon = false
    var maxFindings = Int.MaxValue

    args.sliding(2, 1).toList.collect {
      case Array("--input1", cg) => cg1Path = cg
      case Array("--input2", cg) => cg2Path = cg
      case Array("--maxFindings", max) => maxFindings = max.toInt
      case Array("--showPrecisionRecall", "edges") => showPrecisionRecall = true
    }

    args.sliding(1, 1).toList.collect {
      case Array("--showAdditional") => showAdditional = true
      case Array("--showCommon") => showCommon = true
    }

    if (cg1Path.isEmpty || cg2Path.isEmpty) {
      println("Usage: runMain CompareCGJson --input1 <cg1.json> --input2 <cg2.json> [--showPrecisionRecall edges] [--showAdditional] [--showCommon]")
      sys.exit(1)
    }

    val cg1 = readJson(new File(cg1Path))
    val cg2 = readJson(new File(cg2Path))

    val comparison = compareEdges(cg1, cg2)

    if (showPrecisionRecall) {
      println(f"Edge precision: ${comparison.truePositives}/${comparison.truePositives + comparison.falsePositives} = ${comparison.precision * 100}%.2f%%")
      println(f"Edge recall: ${comparison.truePositives}/${comparison.truePositives + comparison.falseNegatives} = ${comparison.recall * 100}%.2f%%")
      println(f"Edge F1-score: ${comparison.f1 * 100}%.2f")
    }

    if (showAdditional) {
      println(comparison.extraEdges.take(maxFindings).map(_.mkString(" -> ")).mkString(" ##### Additional Edges #####\n\n\t", "\n\t", "\n\n"))
      println(comparison.missingEdges.take(maxFindings).map(_.mkString(" -> ")).mkString(" ##### Missing Edges #####\n\n\t", "\n\t", "\n\n"))
    }

    if (showCommon) {
      val common = comparison.foundEdges.take(maxFindings)
      println(common.map(_.mkString(" -> ")).mkString(" ##### Common Edges #####\n\n\t", "\n\t", "\n\n"))
    }
  }

  private def readJson(file: File): JsonCallGraph = {
    val arr = ujson.read(Source.fromFile(file).mkString).arr
    JsonCallGraph(
      filePath = file.getAbsolutePath,
      links = arr.map(edge => Seq(
        edge("caller")("className").str + ":" + edge("caller")("methodName").str,
        edge("callee")("className").str + ":" + edge("callee")("methodName").str
      )).toSeq
    )
  }

  private def compareEdges(cg1: JsonCallGraph, cg2: JsonCallGraph): ComparisonResult = {
    var foundEdges: Seq[Seq[String]] = Seq()
    var missingEdges: Seq[Seq[String]] = Seq()

    for (edge <- cg1.links) {
      if (cg2.links.exists(e => edgesMatch(edge, e))) {
        foundEdges :+= edge
      } else {
        missingEdges :+= edge
      }
    }

    val extraEdges = cg2.links.filter(e => !cg1.links.exists(ee => edgesMatch(e, ee)))

    val tp = foundEdges.size
    val fn = missingEdges.size
    val fp = extraEdges.size
    val precision = if (tp + fp > 0) tp.toDouble / (tp + fp) else 1.0
    val recall = if (tp + fn > 0) tp.toDouble / (tp + fn) else 1.0
    val f1 = if (precision + recall > 0) 2 * (precision * recall) / (precision + recall) else 0.0

    ComparisonResult(tp, fp, fn, precision, recall, f1, foundEdges, missingEdges, extraEdges)
  }

  private def edgesMatch(e1: Seq[String], e2: Seq[String]): Boolean = {
    e1 == e2
  }
}

import java.io.File

import play.api.libs.json._

import scala.io.Source

object CallGraphComparer extends App {

  val expectedFile = new File("out/jcg_callgraphs_testadapter/LRR1_GroundTruthCG")
  val actualFile   = new File("out/jcg_callgraphs_testadapter/DynamicFilteredCallGraph.json")

  val expectedCG = parseFileToJson(expectedFile).getOrElse {
    println("Fehler beim Einlesen der Ground Truth")
    sys.exit(1)
  }

  val actualCG = parseFileToJson(actualFile).getOrElse {
    println("Fehler beim Einlesen des dynamischen Call Graphs")
    sys.exit(1)
  }

  val expectedEdges = extractEdges(expectedCG)
  val actualEdges   = extractEdges(actualCG)

  // Zähler für Recall
  var matchCounter = 0
  val foundEdges = expectedEdges.filter { case (caller, callee) =>
    val matched = actualEdges.exists { case (aCaller, aCallee) =>
      aCaller.contains(caller) && aCallee.contains(callee)
    }
    if (matched) matchCounter += 1
    matched
  }

  val missingEdges = expectedEdges.diff(foundEdges)
  val extraEdges   = actualEdges.filter { case (aCaller, aCallee) =>
    !expectedEdges.exists { case (caller, callee) =>
      aCaller.contains(caller) && aCallee.contains(callee)
    }
  }

  // Fuzzy Metrics
  val recall    = if (expectedEdges.nonEmpty) matchCounter.toDouble / expectedEdges.size else 1.0
  val precision = if (actualEdges.nonEmpty) matchCounter.toDouble / actualEdges.size else 1.0

  println(s"Gefundene Edges: $matchCounter / ${expectedEdges.size} in GT")
  println(s"Precision: $precision")
  println(s"Recall: $recall")
  println(s"Fehlende Edges (in GT, nicht im dynamischen Graph): $missingEdges")
  println(s"Extra Edges (im dynamischen Graph, nicht in GT): $extraEdges")

  def parseFileToJson(file: File): Option[JsValue] = {
    try {
      val source = Source.fromFile(file)
      val content = try source.mkString finally source.close()
      Some(Json.parse(content))
    } catch {
      case e: Exception =>
        println(s"Fehler beim Parsen von ${file.getName}: ${e.getMessage}")
        None
    }
  }

  def extractEdges(json: JsValue): Set[(String, String)] = {
    val edgesArray = json.asOpt[JsArray].getOrElse(Json.arr())
    edgesArray.value.map { edge =>
      val caller = edge("caller")
      val callee = edge("callee")

      val (callerClass, callerMethod) = normalizeName(
        (caller \ "className").as[String],
        (caller \ "methodName").as[String]
      )
      val (calleeClass, calleeMethod) = normalizeName(
        (callee \ "className").as[String],
        (callee \ "methodName").as[String]
      )

      (s"$callerClass:$callerMethod", s"$calleeClass:$calleeMethod")
    }.toSet
  }

  def normalizeName(className: String, methodName: String): (String, String) = {
    val simpleClass = className.replaceAll("^L|;$", "")
    val simpleMethod = methodName.replaceAll("\\(.*\\)$", "")
    (simpleClass, simpleMethod)
  }
}

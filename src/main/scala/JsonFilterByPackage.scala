/**
 * TODO:
 * packagePrefix ändern, um z.B. "Llrr/" zu filtern
 * Überlegen, ob man der package Name nicht ins code,
 * sondern bei der ausfuehrung geben soll.
 */

import java.io.PrintWriter

import play.api.libs.json.{Format, Json}

import scala.collection.Seq
import scala.io.Source

case class CallRelation(caller: String, callee: String)
object CallRelation {
  implicit val format: Format[CallRelation] = Json.format[CallRelation]
}

object JsonFilterByPackage {
  def main(args: Array[String]): Unit = {
    val inputFile = "src/main/Dynamische Analyse/callgraph.json" // große JSON-Datei
    val outputFile = "out/jcg_callgraphs_testadapter/DynamicLlr1CallGraph.json" //gefiltertes JSON

    val packagePrefix = "Llrr/" // wonach gefiltert werden soll

    //JSON einlesen
    val jsonStr = Source.fromFile(inputFile).mkString
    val jsonArray = Json.parse(jsonStr).as[Seq[CallRelation]]

    // Filtern: nur Einträge behalten, deren caller oder callee mit Prefix anfangen
    val filtered = jsonArray.filter( cr =>
      cr.caller.startsWith(packagePrefix) || cr.callee.startsWith(packagePrefix))

    println(s"Gefilterte Einträge: ${filtered.size}")

    //Gefiltertes JSON in Datei speichern
    val pw = new PrintWriter(outputFile)
    try {
      val jsonToWrite = Json.prettyPrint(Json.toJson(filtered))
      pw.write(jsonToWrite)
    } finally {
      pw.close()
    }
    println(s"Gespeichert JSON gespeichert in: $outputFile")
  }

}
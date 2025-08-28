/**
 * TODO:
 * packagePrefix kann beim Ausführen als Argument angegeben werden
 */

import java.io.PrintWriter

import play.api.libs.json.{Format, Json}

import scala.collection.Seq
import scala.io.Source

case class MethodReference(className: String, methodName: String)
object MethodReference {
  implicit val format: Format[MethodReference] = Json.format[MethodReference]
}

case class CallRelation(caller: MethodReference, callee: MethodReference)
object CallRelation {
  implicit val format: Format[CallRelation] = Json.format[CallRelation]
}

object CallGraphFilter {
  def main(args: Array[String]): Unit = {
    val inputFile = "src/main/Dynamische Analyse/callgraph.json"
    val outputFile = "out/jcg_callgraphs_testadapter/DynamicFilteredCallGraph.json"

    // Package Prefix als Argument oder Default
    val packagePrefix = if (args.nonEmpty) args(0) else "Llrr/"

    println(s"Filtere CallGraph nach Package: $packagePrefix")

    // JSON einlesen
    val jsonStr = Source.fromFile(inputFile).mkString
    val jsonArray = Json.parse(jsonStr).as[Seq[CallRelation]]

    // Filtern: nur Einträge behalten, deren caller oder callee mit Prefix anfangen
    val filtered = jsonArray.filter(cr =>
      cr.caller.className.startsWith(packagePrefix) || cr.callee.className.startsWith(packagePrefix)
    )

    println(s"Gefilterte Einträge: ${filtered.size}")

    // Gefiltertes JSON in Datei speichern
    val pw = new PrintWriter(outputFile)
    try pw.write(Json.prettyPrint(Json.toJson(filtered)))
    finally pw.close()

    println(s"Gefiltertes JSON gespeichert in: $outputFile")
  }
}

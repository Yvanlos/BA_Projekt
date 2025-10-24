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
    if (args.length < 2) {
      println("Usage: CallGraphFilter <inputFile> <outputFile> [packagePrefix]")
      System.exit(1)
    }

    val inputFile = args(0)
    val outputFile = args(1)
    val packagePrefix = if (args.length >= 3) args(2) else "Llrr/"

    println(s"Eingabedatei: $inputFile")
    println(s"Ausgabedatei: $outputFile")
    println(s"Filtere CallGraph nach Package: $packagePrefix")

    val jsonStr = Source.fromFile(inputFile).mkString
    val jsonArray = Json.parse(jsonStr).as[Seq[CallRelation]]

    val filtered = jsonArray.filter(cr =>
      cr.caller.className.startsWith(packagePrefix) || cr.callee.className.startsWith(packagePrefix)
    )

    println(s"Gefilterte Einträge: ${filtered.size}")

    val pw = new PrintWriter(outputFile)
    try pw.write(Json.prettyPrint(Json.toJson(filtered)))
    finally pw.close()

    println(s"Gefiltertes JSON gespeichert in: $outputFile")
  }
}


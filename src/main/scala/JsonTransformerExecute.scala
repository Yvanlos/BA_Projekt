import java.io.{File, PrintWriter}
import scala.io.Source
import play.api.libs.json._

case class Method(className: String, methodName: String)
case class CallerCallee(caller: Method, callee: Method)

object JsonTransformer {
  implicit val methodReads: Reads[Method] = Json.reads[Method]
  implicit val callerCalleeReads: Reads[CallerCallee] = Json.reads[CallerCallee]

  def transform(jsonStr: String): String = {
    val json = Json.parse(jsonStr)

    // "edges" extrahieren
    val edges = (json \ "edges").as[Seq[CallerCallee]]

    def formatClassName(name: String): String =
      s"L${name.replace('.', '/').stripSuffix(";")};"

    def formatMethodName(name: String): String =
      s"$name()V"

    val transformed = edges.map { e =>
      CallerCallee(
        caller = Method(formatClassName(e.caller.className), formatMethodName(e.caller.methodName)),
        callee = Method(formatClassName(e.callee.className), formatMethodName(e.callee.methodName))
      )
    }

    // wieder als JSON
    Json.prettyPrint(Json.toJson(transformed))
  }

  implicit val methodWrites: Writes[Method] = Json.writes[Method]
  implicit val callerCalleeWrites: Writes[CallerCallee] = Json.writes[CallerCallee]
}

object JsonTransformerExecute {
  def main(args: Array[String]): Unit = {
    if (args.length < 2) {
      println("Usage: JsonTransformerApp <InputFile> <OutputFile>")
      sys.exit(1)
    }

    val inputFile = new File(args(0))
    val outputFileName = args(1)

    val fileContent = Source.fromFile(inputFile).getLines().mkString
    val outputJson = JsonTransformer.transform(fileContent)

    val outputFile = new File(inputFile.getParentFile, outputFileName)
    val writer = new PrintWriter(outputFile)
    try writer.write(outputJson)
    finally writer.close()

    println(s"Transformation fertig. Ergebnis in ${outputFile.getAbsolutePath}")
  }
}

/*import java.io.{PrintWriter, File}
import scala.io.Source
import scala.util.parsing.json.JSON

case class Method(className: String, methodName:String)
case class CallerCallee(caller: Method, callee: Method)

object JsonTransformer {

  def transform(jsonStr: String): String = {
    // parse das JSON
    val parsed = JSON.parseFull(jsonStr).getOrElse(Map.empty).asInstanceOf[Map[String, Any]]

    // Hilfsfunktion zum extrahieren von Methoden
    def extractMethods(key: String): List[Method] = {
      parsed.get(key) match {
        case Some(list: List[Map[String, String] @unchecked]) =>
          list.map(m=> Method(m("className"), m("methodName")))
        case _ => Nil
      }
    }

    val callers = extractMethods("callers")
    val callees = extractMethods("callees")

    def formatClassName(name: String): String = s"L${name.replace('.','/').stripSuffix(";")};"
    def formatMethodName(name: String): String = s"$name()V"

    val pairs = for {
      caller <- callers
      callee <- callees
    } yield CallerCallee(
      caller = Method(formatClassName(caller.className), formatMethodName(caller.methodName)),
      callee = Method(formatClassName(callee.className), formatMethodName(callee.methodName))
    )

    val jsonList = pairs.map { pairs =>
      s"""{
            "caller": {"className": "${pairs.caller.className}", "methodName": "${pairs.caller.methodName}"},
            "callee": {"className": "${pairs.callee.className}", "methodName": "${pairs.callee.methodName}"}
          }
        """
    }
    "["+ jsonList.mkString(",")+"]"
  }
}
*/
import java.io.{File, PrintWriter}
import scala.io.Source
import scala.util.parsing.json.JSON

case class Method(className: String, methodName:String)
case class CallerCallee(caller: Method, callee: Method)

object JsonTransformer {
  def transform(jsonStr: String): String = {
    val parsed = JSON.parseFull(jsonStr).getOrElse(Map.empty).asInstanceOf[Map[String, Any]]

    def extractMethods(key: String): List[Method] = {
      parsed.get(key) match {
        case Some(list: List[Map[String, String] @unchecked]) =>
          list.map(m=> Method(m("className"), m("methodName")))
        case _ => Nil
      }
    }

    val callers = extractMethods("callers")
    val callees = extractMethods("callees")

    def formatClassName(name: String): String = s"L${name.replace('.','/').stripSuffix(";")};"
    def formatMethodName(name: String): String = s"$name()V"

    val pairs = for {
      caller <- callers
      callee <- callees
    } yield CallerCallee(
      caller = Method(formatClassName(caller.className), formatMethodName(caller.methodName)),
      callee = Method(formatClassName(callee.className), formatMethodName(callee.methodName))
    )

    val jsonList = pairs.map { pairs =>
      s"""{
            "caller": {"className": "${pairs.caller.className}", "methodName": "${pairs.caller.methodName}"},
            "callee": {"className": "${pairs.callee.className}", "methodName": "${pairs.callee.methodName}"}
          }
        """
    }
    "["+ jsonList.mkString(",")+"]"
  }
}

object JsonTransformerExecute{
  def main(args: Array[String]): Unit = {
    if(args.length < 2){
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

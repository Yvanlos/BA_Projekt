import java.io.{File, PrintWriter}
import scala.io.Source
import play.api.libs.json._

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

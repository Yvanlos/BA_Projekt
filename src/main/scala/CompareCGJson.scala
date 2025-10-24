import java.io.{File, PrintWriter}
import scala.io.Source
import ujson._

object CompareCGJson {

  def readCallGraph(file: File): CallGraph = {
    if (!file.exists()) return CallGraph(file.getName, Set.empty)
    val text = Source.fromFile(file).mkString
    val arr = ujson.read(text).arr
    val edges = arr.map { obj =>
      val callerClass = obj("caller")("className").str
      val callerMethod = obj("caller")("methodName").str
      val calleeClass = obj("callee")("className").str
      val calleeMethod = obj("callee")("methodName").str
      s"$callerClass:$callerMethod -> $calleeClass:$calleeMethod"
    }.toSet
    CallGraph(file.getName, edges)
  }

  def main(args: Array[String]): Unit = {
    val folder = new File("jcg_static_dynamic_cg_testAdapter/out")
    if (!folder.exists() || !folder.isDirectory) {
      println(s"Ordner nicht gefunden: ${folder.getPath}")
      sys.exit(1)
    }

    val files = folder.listFiles().filter(_.getName.endsWith(".json")).toSeq
    if (files.isEmpty) {
      println(s"Keine .json Dateien in ${folder.getPath}")
      sys.exit(1)
    }

    val testNames = files.map(f => f.getName.split("_")(0)).distinct.sorted
    val outCsvDir = new File(folder, "compare_results")
    outCsvDir.mkdirs()

    val csvStaticVsDynamic = new PrintWriter(new File(outCsvDir, "static_vs_dynamic.csv"))
    csvStaticVsDynamic.println("Test;DynSize;StatSize;DynamicFeatures;StaticFound;DynamicFound")

    println("Vergleich Call-Graphen")
    println("-" * 120)

    for (test <- testNames) {
      val dynOpt = files.find(_.getName == s"${test}_dynamic.json")
      val statOpt = files.find(_.getName == s"${test}_static.json")

      val dyn = dynOpt.map(readCallGraph).getOrElse(CallGraph("", Set.empty))
      val stat = statOpt.map(readCallGraph).getOrElse(CallGraph("", Set.empty))

      // Dynamische Features prüfen ohne Regex
      val features = DynamicFeatures.dynamicFeatures.getOrElse(test, Seq.empty)

      val statMethods = stat.edges.map(e => e.split(" -> ")(1).split(":")(1))
      val dynMethods = dyn.edges.map(e => e.split(" -> ")(1).split(":")(1))

      val statFound = features.filter(f => {
        val parts = f.split("\\.")
        val methodName = if (parts.length > 1) parts(1) else parts(0)
        statMethods.exists(_.contains(methodName))
      }).mkString(", ")

      val dynFound = features.filter(f => {
        val parts = f.split("\\.")
        val methodName = if (parts.length > 1) parts(1) else parts(0)
        dynMethods.exists(_.contains(methodName))
      }).mkString(", ")

      csvStaticVsDynamic.println(
        s"$test;${dyn.edges.size};${stat.edges.size};\"${features.mkString(",")}\";\"$statFound\";\"$dynFound\""
      )
    }

    csvStaticVsDynamic.close()
    println("\nCSV-Datei geschrieben nach: " + outCsvDir.getAbsolutePath)
  }
}
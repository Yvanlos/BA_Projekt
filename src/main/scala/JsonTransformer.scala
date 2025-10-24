import play.api.libs.json.{Json, Reads, Writes}


object JsonTransformer {
  case class Method(className: String, methodName: String)
  case class CallerCallee(caller: Method, callee: Method)


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

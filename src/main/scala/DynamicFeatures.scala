object DynamicFeatures {
  val dynamicFeatures: Map[String, Seq[String]] = Map(
    "TR1" -> Seq("Class.getDeclaredMethod", "Method.invoke"),
    "TR2" -> Seq("Class.getDeclaredMethod", "Method.invoke"),
    "TR3" -> Seq("Class.getMethod", "Method.invoke"),
    "TR4" -> Seq("Class.getDeclaredMethod", "Method.invoke"),
    "TR5" -> Seq("Class.newInstance"),
    "TR6" -> Seq("Class.getConstructor","Constructor.newInstance"),
    "TR7" -> Seq("Class.getDeclaredField","Field.get"),
    "TR8" -> Seq("Class.getField",".Field.get"),
    "TR9" -> Seq("Class.forName","staticInitializerCalled"),
    "LRR1" -> Seq("Class.forName","staticInitializerCalled"),
    "LRR2" -> Seq("Class.forName","Ljava/lang/StringBuilder"),
    "LRR3" -> Seq("Class.forName"),
    "CSR1" -> Seq("Class.forName", "inter-procedural string propagation"),
    "CSR2" -> Seq("Class.forName", "unknown input string"),
    "CSR3" -> Seq("Class.forName","static field propagation"),
    "CSR4" -> Seq("Class.forName","System.getProperty","System.setProperties"),
    "dp"->Seq("Proxy.newProxyInstance","InvocationHandler.invoke","Method.invoke"),
    "JVMC1" -> Seq(
      "Runtime.addShutdownHook", // Hook-Thread registrieren
      "Thread.start",            // Thread wird gestartet
      "Runnable.run",            // run() wird vom JVM aufgerufen
      "callback"                 // die annotierte Callback-Methode
    ),
    "JVMC2" -> Seq(
      "finalize",   // finalize-Methode, aufgerufen durch Garbage Collector
      "callback"    // die annotierte Callback-Methode
    ),
    "JVMC3" -> Seq(
      "Thread.start",           // Thread wird gestartet
      "Thread.run",             // implizite Aufrufkante durch JVM
      "verifyReachability"      // test-spezifische Methode, die die Erreichbarkeit prüft
    ),
    "JVMC4" -> Seq(
      "Thread.start",           // Thread wird gestartet
      "Thread.exit"             // implizite Methode beim Thread-Exit
    ),
    "JVMC5" -> Seq(
      "Thread.setUncaughtExceptionHandler",   // Handler registrieren
      "Thread.dispatchUncaughtException",     // JVM ruft diese Methode bei Ausnahme auf
      "callback"                              // annotierte Callback-Methode
    )
  )
}
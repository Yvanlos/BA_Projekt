package jcg_callgraphs_testadapter

import jcg_callgraphs_testadapter.static.StaticRunner


class MainAdapter {
  def main(args: Array[String]): Unit = {
    println("========= Adapter gestartet =========")
    println("---> Statische Analyse")
    StaticRunner.run();

    //hier implementierung der Dynamic run()

  }
}

# Scala & OPAL Setup Test
Dieses Verzeichnis enthält ein Demo-Projekt, dass dazu gedacht ist ihr lokales Setup von Scala, SBT, Java und OPAL zu testen. Mit korrekt eingerichteter Umgebung sollten sie die folgenden Schritte ausführen können.

Öffnen Sie eine Kommandozeile im aktuellen Verzeichnis und führen sie die folgenden Kommandos aus:

```
sbt
```
**Erwartetes Verhalten:** *Nach kurzer Ladezeit öffnet sich die SBT-Kommandozeile und wartet auf Eingaben.*

```
clean
```
**Erwartetes Verhalten:** *SBT räumt Build-Artefakte auf, d.h. dass Inhalte im `target` Verzeichnis gelöscht werden*

```
compile
```
**Erwartetes Verhalten:** *SBT kompiliert das Demo-Projekt ohne Fehler**

```
run -cp=HelloWorld.jar
```
**Erwartetes Verhalten:** *Das Demoprojekt enthält eine einfache statische Analyse. Diese wird auf dem Beispielcode in der Datei `HelloWorld.jar` ausgeführt*
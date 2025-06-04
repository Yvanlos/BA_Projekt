
# Dynamic call Graph start & tcp-serialisation

## listener.py -Kompilieren mit Python 2.7.16

**hinweis: eretze in den Befehl LRR1.jar durch die jeweilige Jar-Datei, die du verwenden möchtest**


##auf macOS
***Dynamic Agent Kompilieren***

```
g++ -std=c++11 -fPIC -shared -o DynamicCG.dylib DynamicCG.cpp \
  -I"$JAVA_HOME/include" \
  -I"$JAVA_HOME/include/darwin" \
  -ldl
```

***server Starten and Serialisierung einrichten***

```
python listener.py
```
***tcp Verbindung zum Server über python Lystener herstellen***
```
java -agentpath:DynamicCG.dylib=1337 -jar LRR1.jar
```
##auf linux

***Dynamic Agent Kompilieren***
```
g++ -std=c++11 -fPIC -shared -o DynamicCG.so DynamicCG.cpp \
  -I"$JAVA_HOME/include" \
  -I"$JAVA_HOME/include/darwin" \
  -ldl
```

***server Starten and Serialisierung einrichten***

```
python listener.py
```
***tcp Verbindung zum Server über python Lystener herstellen***
```
java -agentpath:DynamicCG.so=1337 -jar /Input/LRR1.jar
```

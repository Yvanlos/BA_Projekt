# Java Agent Call Graph Builder

This code builds a **Java Agent** written in **C++** using the **JVMTI (Java Virtual Machine Tool Interface)**.

It hooks into a running JVM to:
- Track method calls (who calls whom)
- Build a call graph (who calls who, when, and where)
- Send this data as JSON over a network socket (to `localhost`)

## Key Components

### 1- Data Structure

```cpp
struct Callsite {
    int topLevel;      // 1 if top-level (no caller)
    jmethodID mid;     // ID of the caller method
    jlocation loc;     // location in the bytecode

    // Defines equality and ordering so Callsite can be used
    bool operator==(const Callsite &o) const { ... }
    bool operator<(const Callsite &o) const { ... }

    // Callsite: represents where a method call happened (e.g. line number)
};
```

####cg is a map:

```cpp
map<Callsite, unordered_set<jmethodID>> cg;
```
- Maps a **callsite** to all the methods it called

### 2- getMethodNameSig

```cpp
getMethodNameSig(jmethodID mid, char** nameSig)
```
- Fetches the method ***name and signature*** as a string 
(e.g. Lcom/Foo;:bar(I)V)

### 3- MethodEntry callback
```cpp
void JNICALL MethodEntry(jvmtiEnv*, JNIEnv*, jthread, jmethodID method)
```
- Runs on every method entry
- Records the caller + callee in the cg map.

### 4- return_cg
When the JVM dies:
- Connect to 127.0.0.1_port
- Send JSON of call graph

Example JSON:  

```json
[
  { "caller": "TopLevel", "callee": "Lcom/Foo;:main([Ljava/lang/String;)V" },
  { "caller": "Lcom/Foo;:main([Ljava/lang/String;)V", "callee": "Lcom/Foo;:doWork()V" }
]
```
### Agent_Onload
- Enter when agent is loaded
- registers callbacks and anable JVMTI events


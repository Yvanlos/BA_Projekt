//#define _GNU_SOURCE

#include <nlohmann/json.hpp>
using json = nlohmann::json;

#include <jvmti.h>
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <map>
#include <unordered_set>
#include <arpa/inet.h>
#include <sys/socket.h>
#include <unistd.h>
#include <cstring>
#include <sstream>

using namespace std;

static jvmtiEnv *jvmti = NULL;
static int port = 0;

struct Callsite {
    int topLevel;
    jmethodID mid;
    jlocation loc;

    bool operator==(const Callsite &o) const {
        return topLevel == o.topLevel && mid == o.mid && loc == o.loc;
    }

    bool operator<(const Callsite &o) const {
        return topLevel < o.topLevel || topLevel == o.topLevel && (mid < o.mid || mid == o.mid && loc < o.loc);
    }
};

static map<Callsite, unordered_set<jmethodID>> cg;

static void getMethodNameSig(jmethodID mid, char** nameSig) {
    jclass cls;
    int err = jvmti->GetMethodDeclaringClass(mid, &cls);

    if(err == JVMTI_ERROR_NONE){
        char* cname;
        char* generic;
        jvmti->GetClassSignature(cls, &cname, &generic);

        jvmti->Deallocate((unsigned char*) generic);

        char* mname;
        char* sig;
        jvmti->GetMethodName(mid, &mname, &sig, &generic);

        asprintf(nameSig, "%s:%s%s", (cname == NULL ? "" : cname), (mname == NULL ? "" : mname), (sig == NULL ? "" : sig));

        jvmti->Deallocate((unsigned char*) cname);
        jvmti->Deallocate((unsigned char*) mname);
        jvmti->Deallocate((unsigned char*) sig);
        jvmti->Deallocate((unsigned char*) generic);
    } else {
        asprintf(nameSig, "<FAILED>");
    }
}
void return_cg() {
    int channel;
    struct sockaddr_in serv_addr;

    channel = socket(AF_INET, SOCK_STREAM, 0);

    serv_addr.sin_family = AF_INET;
    serv_addr.sin_port = htons(port);
    inet_pton(AF_INET, "127.0.0.1", &serv_addr.sin_addr);
    connect(channel, (struct sockaddr*)&serv_addr, sizeof(serv_addr));

    // JSON Array für alle Kanten
    json j_callgraph = json::array();

    for (const auto& calls : cg) {
        const Callsite callsite = calls.first;

        char* caller;
        if (callsite.topLevel == 1) {
            caller = strdup("TopLevel:TopLevel");
        } else {
            getMethodNameSig(callsite.mid, &caller);
        }

        const unordered_set<jmethodID>& calleesSet = calls.second;

        for (const jmethodID mid : calleesSet) {
            char* callee;
            getMethodNameSig(mid, &callee);

            auto splitSig = [](const string& full) {
                size_t sep = full.find(':');
                string cls = full.substr(0, sep);
                string meth = (sep != string::npos) ? full.substr(sep + 1) : "";
                return make_pair(cls, meth);
            };

            string callerStr(caller);
            string calleeStr(callee);

            auto cSplit = splitSig(callerStr);
            auto eSplit = splitSig(calleeStr);

            // JSON-Eintrag für Kante
            json edge = {
                {"caller", {
                    {"methodName", cSplit.second},
                    {"className",  cSplit.first}
                }},
                {"callee", {
                    {"methodName", eSplit.second},
                    {"className",  eSplit.first}
                }}
            };

            j_callgraph.push_back(edge);

            free(callee);
        }

        if (callsite.topLevel == 0) {
            free(caller);
        }
    }

    // Nur das Array senden
    std::string json_str = j_callgraph.dump(2); // schön formatiert
    send(channel, json_str.c_str(), json_str.size(), 0);

    close(channel);
}




void JNICALL MethodEntry(jvmtiEnv *jvmti, JNIEnv* jni, jthread thread, jmethodID method) {
    jlocation loc;

    // Get the caller method and call location from the previous stack frame
    jmethodID caller;
    int err = jvmti->GetFrameLocation(NULL, 1, &caller, &loc);

    Callsite callsite;
    if(err==JVMTI_ERROR_NO_MORE_FRAMES) {
        callsite = Callsite{1, NULL, 0};
    } else {
        callsite = Callsite{0, caller, loc};
    }

    auto& callees = cg.emplace(make_pair(move(callsite), unordered_set<jmethodID>())).first->second;
    callees.insert(method);
}

JNIEXPORT void JNICALL VMDeath(jvmtiEnv *jvmti_env, JNIEnv* jni_env) {
    return_cg();
}

JNIEXPORT jint JNICALL Agent_OnLoad(JavaVM *vm, char *options, void *reserved) {
    port = atoi(options);

    vm->GetEnv((void**)&jvmti, JVMTI_VERSION_1_0);

    jvmtiCapabilities capabilities = {0};
    capabilities.can_generate_method_entry_events = 1;
    capabilities.can_get_line_numbers = 1;
    jvmti->AddCapabilities(&capabilities);

    jvmtiEventCallbacks callbacks = {0};
    callbacks.MethodEntry = MethodEntry;
    callbacks.VMDeath = VMDeath;
    jvmti->SetEventCallbacks(&callbacks, sizeof(callbacks));

    jvmti->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_METHOD_ENTRY, NULL);
    jvmti->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_VM_DEATH, NULL);

    return 0;
}

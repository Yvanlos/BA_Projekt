# -*- coding: utf-8 -*-
import socket
import json

PORT = 1337

s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
s.bind(('127.0.0.1', PORT))
s.listen(1)
print "Listening on port %d..." % PORT

conn, addr = s.accept()
print "Connected by", addr

buffer = ""
while True:
    data = conn.recv(4096)
    if not data:
        break
    buffer += data

conn.close()

# JSON-String aus Bytes decodieren und parsen
json_str = buffer.decode('utf-8')
callgraph = json.loads(json_str)

# JSON in Datei speichern (schön formatiert)
with open("callgraph.json", "w") as f:
    json.dump(callgraph, f, indent=2)

print("Callgraph saved to callgraph.json")
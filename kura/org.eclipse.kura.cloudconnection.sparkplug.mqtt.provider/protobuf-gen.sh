#!/bin/bash
protoc --proto_path=src/main/java --java_out=src/main/java src/main/java/org/eclipse/kura/cloudconnection/sparkplug/mqtt/message/protobuf/sparkplug_b.proto
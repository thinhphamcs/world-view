#!/bin/bash
if kafka-storage.sh info -c ./server.properties; then
    echo "Kafka data directory exists. Skipping storage formatting."
else
    echo "Kafka data directory does not exist. Formatting storage..."
    kafka-storage format -t $(kafka-storage random-uuid) -c ./server.properties
fi
kafka-server-start.sh ./server.properties
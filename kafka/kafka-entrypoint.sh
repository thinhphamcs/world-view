#!/bin/bash
if /opt/kafka/bin/kafka-storage.sh info -c ./server.properties; then
    echo "Kafka data directory exists. Skipping storage formatting."
else
    echo "Kafka data directory does not exist. Formatting storage..."
    /opt/kafka/bin/kafka-storage.sh format -t $(/opt/kafka/bin/kafka-storage.sh random-uuid) -c ./server.properties
fi
/opt/kafka/bin/kafka-server-start.sh ./server.properties
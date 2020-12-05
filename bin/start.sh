#!/bin/bash

set -e

until curl http://$MOONLIGHT_NEO4J_HOST:7474; do
  >&2 echo "Neo4j is unavailable - Sleeping..."
  sleep 1
done

>&2 echo "\n\nNeo4j is up - Checking password..."

INITIAL_PASSWORD_AUTH_ERROR=$(curl -u neo4j:neo4j http://$MOONLIGHT_NEO4J_HOST:7474/user/neo4j | grep "errors" | wc -l | sed 's/ //g')

# if password is still neo4j (no errors from above (=0)), change the password
if [[ $INITIAL_PASSWORD_AUTH_ERROR == 0 ]]; then
    echo "Initial password is still configured. Changing it now..."

    curl -u neo4j:neo4j -X POST http://$MOONLIGHT_NEO4J_HOST:7474/user/neo4j/password -d "password=$MOONLIGHT_NEO4J_PASSWORD"
elif [[ $INITIAL_PASSWORD_AUTH_ERROR == 1 ]]; then
    echo "Initial password is changed already!"
fi

java -jar $ARTIFACT_PATH

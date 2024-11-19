#!/bin/bash
cd "$(dirname "$0")/demo"

# Ensure the script is executed from the project root
PROJECT_ROOT="$(cd "$(dirname "$0")" && pwd)"
cd "$PROJECT_ROOT"

# Locate the shaded JAR file
JAR_FILE=$(find target -name "*.jar" | head -n 1)

if [ -z "$JAR_FILE" ]; then
    echo "Error: No shaded JAR file found. Make sure the project is built correctly using 'mvn clean package'."
    exit 1
fi

# Run the JAR file
echo "Running JAR: $JAR_FILE"
java -jar "$JAR_FILE"

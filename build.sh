#!/bin/bash
cd "$(dirname "$0")/demo"

# Set the name of the log file
LOG_FILE="build.log"

# Print the start message
echo "Starting compilation for 417 Interpreter."

# Check if Maven is installed
if ! command -v mvn &> /dev/null
then
    echo "Maven is not installed. Please install Maven before proceeding."
    exit 1
fi

# Clean, compile, and install the Maven project
echo "Running mvn clean install..."
mvn clean install > $LOG_FILE 2>&1

# Check if the build was successful
if [ $? -eq 0 ]; then
    echo "Build succeeded."
else
    echo "Build failed. Check $LOG_FILE for details."
    exit 1
fi

# Final success message
echo "Maven build completed successfully."
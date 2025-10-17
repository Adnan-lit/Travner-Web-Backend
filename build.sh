#!/bin/bash

# Railway build script for Spring Boot application
echo "🚀 Building Travner Backend for Railway..."

# Set Java version
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk

# Build the application
echo "📦 Building JAR file..."
./mvnw clean package -DskipTests

# Check if build was successful
if [ $? -eq 0 ]; then
    echo "✅ Build successful!"
    echo "📁 JAR file created: target/Travner-0.0.1-SNAPSHOT.jar"
else
    echo "❌ Build failed!"
    exit 1
fi

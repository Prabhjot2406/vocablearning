#!/bin/bash

echo "Waiting for Ollama service to be ready..."
sleep 10

echo "Pulling llama3.2 model..."
docker exec vocablearning-ollama ollama pull llama3.2

echo "Model pulled successfully! Your application is ready to use."

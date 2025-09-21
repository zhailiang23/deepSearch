#!/bin/bash

ENV=${1:-dev}

echo "=== Health Check for $ENV Environment ==="

case $ENV in
    dev)
        COMPOSE_FILE="docker-compose.yml"
        BACKEND_URL="http://localhost:8080"
        FRONTEND_URL="http://localhost:3000"
        ;;
    test)
        COMPOSE_FILE="docker-compose.test.yml"
        BACKEND_URL="http://localhost:8081"
        FRONTEND_URL="http://localhost:3001"
        ;;
    prod)
        COMPOSE_FILE="docker-compose.prod.yml"
        BACKEND_URL="http://localhost"
        FRONTEND_URL="http://localhost"
        ;;
    *)
        echo "Usage: $0 {dev|test|prod}"
        exit 1
        ;;
esac

echo "Checking container status..."
docker-compose -f $COMPOSE_FILE ps

echo -e "\nChecking backend health..."
if curl -f -s $BACKEND_URL/actuator/health > /dev/null; then
    echo "✅ Backend is healthy"
else
    echo "❌ Backend health check failed"
fi

echo -e "\nChecking frontend..."
if curl -f -s $FRONTEND_URL > /dev/null; then
    echo "✅ Frontend is accessible"
else
    echo "❌ Frontend is not accessible"
fi

echo -e "\nContainer logs (last 10 lines):"
docker-compose -f $COMPOSE_FILE logs --tail=10
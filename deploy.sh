#!/bin/bash

set -e

ENV=${1:-dev}
ACTION=${2:-up}

echo "=== Basic Management System Deployment Script ==="
echo "Environment: $ENV"
echo "Action: $ACTION"
echo "================================================="

case $ENV in
    dev)
        COMPOSE_FILE="docker-compose.yml"
        echo "Using development environment configuration"
        ;;
    test)
        COMPOSE_FILE="docker-compose.test.yml"
        echo "Using test environment configuration"
        ;;
    prod)
        COMPOSE_FILE="docker-compose.prod.yml"
        echo "Using production environment configuration"
        # 检查环境变量文件
        if [ ! -f ".env" ]; then
            echo "Error: .env file not found for production deployment"
            echo "Please copy .env.example to .env and configure it"
            exit 1
        fi
        ;;
    *)
        echo "Usage: $0 {dev|test|prod} {up|down|restart|logs|build|clean}"
        echo ""
        echo "Commands:"
        echo "  up      - Start services"
        echo "  down    - Stop services"
        echo "  restart - Restart services"
        echo "  logs    - Show logs"
        echo "  build   - Build and start services"
        echo "  clean   - Stop and remove all containers, networks, and volumes"
        exit 1
        ;;
esac

case $ACTION in
    up)
        echo "Starting $ENV environment..."
        docker-compose -f $COMPOSE_FILE up -d
        echo "Services started successfully!"
        echo "You can check the status with: docker-compose -f $COMPOSE_FILE ps"
        ;;
    down)
        echo "Stopping $ENV environment..."
        docker-compose -f $COMPOSE_FILE down
        echo "Services stopped successfully!"
        ;;
    restart)
        echo "Restarting $ENV environment..."
        docker-compose -f $COMPOSE_FILE restart
        echo "Services restarted successfully!"
        ;;
    logs)
        echo "Showing logs for $ENV environment..."
        docker-compose -f $COMPOSE_FILE logs -f
        ;;
    build)
        echo "Building and starting $ENV environment..."
        docker-compose -f $COMPOSE_FILE up --build -d
        echo "Services built and started successfully!"
        ;;
    clean)
        echo "Cleaning up $ENV environment..."
        echo "This will remove all containers, networks, and volumes!"
        read -p "Are you sure? (y/N) " -n 1 -r
        echo
        if [[ $REPLY =~ ^[Yy]$ ]]; then
            docker-compose -f $COMPOSE_FILE down -v --remove-orphans
            docker system prune -f
            echo "Cleanup completed!"
        else
            echo "Cleanup cancelled."
        fi
        ;;
    *)
        echo "Usage: $0 {dev|test|prod} {up|down|restart|logs|build|clean}"
        exit 1
        ;;
esac
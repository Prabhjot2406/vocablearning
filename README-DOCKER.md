# 🐳 Docker Deployment Guide

## Prerequisites
- Docker Desktop installed
- Docker Compose installed (comes with Docker Desktop)

## 🚀 Quick Start (For Your Client)

### Step 1: Start All Services
```bash
docker-compose up --build
```

This will:
- ✅ Build your Spring Boot application
- ✅ Start MySQL database
- ✅ Start Ollama AI service
- ✅ Start your application

### Step 2: Setup Ollama Model (One-time only)

**Open a new terminal** and run:
```bash
chmod +x setup-ollama.sh
./setup-ollama.sh
```

Or manually:
```bash
docker exec vocablearning-ollama ollama pull llama3.2
```

### Step 3: Access Application
Open browser: **http://localhost:8080**

---

## 🛑 Stop Services
```bash
docker-compose down
```

To remove all data (database + AI models):
```bash
docker-compose down -v
```

---

## 📊 What's Running?

| Service | Port | Container Name |
|---------|------|----------------|
| Spring Boot App | 8080 | vocablearning-app |
| MySQL Database | 3306 | vocablearning-mysql |
| Ollama AI | 11434 | vocablearning-ollama |

---

## 🔍 Troubleshooting

### Check container logs:
```bash
# All services
docker-compose logs

# Specific service
docker-compose logs app
docker-compose logs mysql
docker-compose logs ollama
```

### Check if containers are running:
```bash
docker-compose ps
```

### Restart services:
```bash
docker-compose restart
```

### Rebuild application after code changes:
```bash
docker-compose up --build app
```

---

## 📦 What Gets Installed?

Your client doesn't need to install:
- ❌ Java
- ❌ Maven
- ❌ MySQL
- ❌ Ollama

Only needs:
- ✅ Docker Desktop

---

## 🎯 Architecture

```
┌─────────────────────────────────────────┐
│         Docker Compose Network          │
│                                         │
│  ┌──────────────┐                      │
│  │   Browser    │                      │
│  └──────┬───────┘                      │
│         │ :8080                        │
│  ┌──────▼───────────┐                 │
│  │  Spring Boot App │                 │
│  │  (vocablearning) │                 │
│  └──────┬───────┬───┘                 │
│         │       │                      │
│    :3306│       │:11434               │
│         │       │                      │
│  ┌──────▼─────┐ │  ┌────────────┐    │
│  │   MySQL    │ └─►│   Ollama   │    │
│  │  Database  │    │  AI Service│    │
│  └────────────┘    └────────────┘    │
│                                         │
└─────────────────────────────────────────┘
```

All services communicate using container names (mysql, ollama, app).

---

## 💾 Data Persistence

Data is stored in Docker volumes:
- `mysql-data` - Database data persists between restarts
- `ollama-data` - AI models persist between restarts

Even if you stop containers, data remains until you run `docker-compose down -v`.

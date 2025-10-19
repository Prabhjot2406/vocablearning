# 🐳 Docker Compose - Complete Explanation

## 🎯 What is Docker Compose?

**Docker Compose** is a tool for defining and running multi-container Docker applications using a YAML configuration file.

### **Without Docker Compose:**
```bash
# Start MySQL
docker run -d --name mysql -e MYSQL_ROOT_PASSWORD=pass -p 3306:3306 mysql:8.0

# Start Ollama
docker run -d --name ollama -p 11434:11434 ollama/ollama

# Build and start app
docker build -t myapp .
docker run -d --name app -p 8080:8080 --link mysql --link ollama myapp

# 😫 Too many commands!
# 😫 Hard to remember all options!
# 😫 Error-prone!
```

### **With Docker Compose:**
```bash
docker-compose up
# ✅ One command starts everything!
# ✅ Configuration in one file!
# ✅ Easy to maintain!
```

---

## 📄 Your docker-compose.yml File - Line by Line

```yaml
version: '3.8'
```

### **`version: '3.8'`**
- **What:** Docker Compose file format version
- **Why:** Different versions support different features
- **3.8:** Released 2019, supports most modern features
- **Note:** Version 3.8 is compatible with Docker Engine 19.03.0+

**Version History:**
- `3.0` - Basic features
- `3.8` - Health checks, build args, secrets
- `3.9` - Latest in v3 series

---

## 🔧 Services Section

```yaml
services:
```

**What:** Defines all containers (services) in your application

**Your app has 3 services:**
1. `mysql` - Database
2. `ollama` - AI service
3. `app` - Spring Boot application

---

## 🗄️ Service 1: MySQL Database

```yaml
  mysql:
    image: mysql:8.0
    container_name: vocablearning-mysql
    environment:
      MYSQL_ROOT_PASSWORD: Samsung@11
      MYSQL_DATABASE: vocablearning
    ports:
      - "3306:3306"
    volumes:
      - mysql-data:/var/lib/mysql
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost"]
      timeout: 20s
      retries: 10
```

### **Line-by-Line Breakdown:**

#### **`mysql:`**
- **What:** Service name (you choose this)
- **Used for:** 
  - Container networking: `jdbc:mysql://mysql:3306/`
  - Docker Compose commands: `docker-compose logs mysql`

---

#### **`image: mysql:8.0`**
- **What:** Docker image to use
- **Format:** `repository:tag`
- **mysql:8.0** means:
  - Repository: `mysql` (official MySQL image)
  - Tag: `8.0` (MySQL version 8.0)
- **Alternative:** `mysql:latest` (always gets newest version)

**Where it comes from:**
```
Docker Hub (hub.docker.com)
    ↓
Downloads mysql:8.0 image
    ↓
Creates container from image
```

---

#### **`container_name: vocablearning-mysql`**
- **What:** Custom name for the container
- **Without this:** Docker generates random name like `vocablearning_mysql_1`
- **With this:** Container is always named `vocablearning-mysql`
- **Benefits:**
  - Easy to identify: `docker ps` shows clear name
  - Easy to manage: `docker logs vocablearning-mysql`
  - Consistent across restarts

---

#### **`environment:`**
```yaml
environment:
  MYSQL_ROOT_PASSWORD: Samsung@11
  MYSQL_DATABASE: vocablearning
```

**What:** Environment variables passed to container

**How it works:**
```
Docker Compose sets these variables
    ↓
MySQL container reads them on startup
    ↓
MySQL initializes with these settings
```

**`MYSQL_ROOT_PASSWORD: Samsung@11`**
- Sets root user password
- Required by MySQL image
- Used by your app to connect

**`MYSQL_DATABASE: vocablearning`**
- Creates database named "vocablearning" on first startup
- Your app connects to this database
- Equivalent to: `CREATE DATABASE vocablearning;`

**Other MySQL environment variables (not used):**
- `MYSQL_USER` - Create additional user
- `MYSQL_PASSWORD` - Password for additional user
- `MYSQL_ALLOW_EMPTY_PASSWORD` - Allow empty root password

---

#### **`ports:`**
```yaml
ports:
  - "3306:3306"
```

**What:** Port mapping between host and container

**Format:** `"HOST_PORT:CONTAINER_PORT"`

**Breakdown:**
```
Your Mac:3306  ←→  Container:3306
    ↑                    ↑
  Host port         Container port
```

**What this means:**
- Container MySQL listens on port 3306 (inside container)
- Your Mac can access it on localhost:3306
- Other containers can access it on mysql:3306

**Example:**
```bash
# From your Mac
mysql -h localhost -P 3306 -u root -p

# From app container
mysql -h mysql -P 3306 -u root -p
```

**Port mapping visualization:**
```
┌─────────────────────────────────────┐
│         Your Mac (Host)             │
│                                     │
│  localhost:3306 ─────┐              │
│                      │              │
└──────────────────────┼──────────────┘
                       │
                       ↓ Port mapping
┌──────────────────────┼──────────────┐
│    Docker Network    │              │
│                      │              │
│  ┌───────────────────▼───────────┐  │
│  │  MySQL Container              │  │
│  │  mysql:3306                   │  │
│  │  (listening on port 3306)     │  │
│  └───────────────────────────────┘  │
└─────────────────────────────────────┘
```

---

#### **`volumes:`**
```yaml
volumes:
  - mysql-data:/var/lib/mysql
```

**What:** Mounts named volume to container directory

**Format:** `VOLUME_NAME:CONTAINER_PATH`

**Breakdown:**
- `mysql-data` - Named volume (defined at bottom of file)
- `/var/lib/mysql` - Where MySQL stores data inside container

**Why needed:**
- MySQL stores database files in `/var/lib/mysql`
- Without volume: Data lost when container stops
- With volume: Data persists across restarts

**Data flow:**
```
MySQL writes data
    ↓
/var/lib/mysql (inside container)
    ↓
Mounted to mysql-data volume
    ↓
Data persists on host
```

---

#### **`healthcheck:`**
```yaml
healthcheck:
  test: ["CMD", "mysqladmin", "ping", "-h", "localhost"]
  timeout: 20s
  retries: 10
```

**What:** Checks if MySQL is ready to accept connections

**Why needed:**
- MySQL takes time to initialize (5-30 seconds)
- App shouldn't start until MySQL is ready
- Prevents connection errors

**How it works:**
```
Container starts
    ↓
Docker runs health check every few seconds
    ↓
Runs: mysqladmin ping -h localhost
    ↓
If successful: Container marked "healthy"
    ↓
App container can now start (depends_on)
```

**Parameters:**

**`test: ["CMD", "mysqladmin", "ping", "-h", "localhost"]`**
- Command to check health
- `mysqladmin ping` - MySQL utility to test connection
- Returns 0 if MySQL is ready, non-zero if not

**`timeout: 20s`**
- How long to wait for health check command
- If command takes >20s, it's considered failed

**`retries: 10`**
- How many times to retry before marking unhealthy
- Total wait time: ~10 retries × interval = ~50 seconds

**Health check states:**
```
starting → healthy → running
    ↓
starting → unhealthy → restarting
```

---

## 🤖 Service 2: Ollama AI

```yaml
  ollama:
    image: ollama/ollama:latest
    container_name: vocablearning-ollama
    ports:
      - "11434:11434"
    volumes:
      - ollama-data:/root/.ollama
```

### **Line-by-Line Breakdown:**

#### **`ollama:`**
- Service name for AI service
- Used in app's environment: `http://ollama:11434`

---

#### **`image: ollama/ollama:latest`**
- **Repository:** `ollama/ollama` (official Ollama image)
- **Tag:** `latest` (always pulls newest version)
- **Size:** ~500MB base image
- **Contains:** Ollama server and runtime

---

#### **`container_name: vocablearning-ollama`**
- Custom name for easy identification
- Used in commands: `docker exec vocablearning-ollama ollama pull llama3.2`

---

#### **`ports: "11434:11434"`**
- Ollama's default HTTP port
- Your Mac: `http://localhost:11434`
- App container: `http://ollama:11434`

**API endpoints:**
```
POST http://ollama:11434/api/generate
POST http://ollama:11434/api/chat
GET  http://ollama:11434/api/tags
```

---

#### **`volumes: ollama-data:/root/.ollama`**
- Stores AI models (4GB+ per model)
- `/root/.ollama` - Ollama's data directory
- Without volume: Re-download models on every restart
- With volume: Models persist

**What's stored:**
```
/root/.ollama/
├── models/
│   └── llama3.2/          ← 4GB+ model files
├── manifests/
└── blobs/
```

---

## 🚀 Service 3: Spring Boot App

```yaml
  app:
    build: .
    container_name: vocablearning-app
    ports:
      - "8080:8080"
    environment:
      SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/vocablearning
      SPRING_DATASOURCE_USERNAME: root
      SPRING_DATASOURCE_PASSWORD: Samsung@11
      SPRING_AI_OLLAMA_BASE_URL: http://ollama:11434
    depends_on:
      mysql:
        condition: service_healthy
      ollama:
        condition: service_started
```

### **Line-by-Line Breakdown:**

#### **`app:`**
- Service name for your Spring Boot application

---

#### **`build: .`**
- **What:** Build image from Dockerfile
- **`.`** means current directory
- **Looks for:** `./Dockerfile`

**What happens:**
```
docker-compose up
    ↓
Reads Dockerfile in current directory
    ↓
Builds image (runs Maven, creates JAR)
    ↓
Creates container from built image
```

**Alternative (using pre-built image):**
```yaml
image: myapp:1.0    # Use existing image instead of building
```

---

#### **`container_name: vocablearning-app`**
- Custom name for your app container
- Access logs: `docker logs vocablearning-app`

---

#### **`ports: "8080:8080"`**
- Spring Boot's default port
- Access app: `http://localhost:8080`

---

#### **`environment:`**
```yaml
environment:
  SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/vocablearning
  SPRING_DATASOURCE_USERNAME: root
  SPRING_DATASOURCE_PASSWORD: Samsung@11
  SPRING_AI_OLLAMA_BASE_URL: http://ollama:11434
```

**What:** Overrides values in `application.properties`

**How it works:**
```
application.properties has:
spring.datasource.url=jdbc:mysql://localhost:3306/vocablearning

Environment variable overrides it:
SPRING_DATASOURCE_URL=jdbc:mysql://mysql:3306/vocablearning
                                    ↑
                            Uses service name, not localhost!
```

**Why override:**
- `application.properties` uses `localhost` (for local development)
- Docker needs service names (`mysql`, `ollama`)
- Environment variables override without changing code

**Variable breakdown:**

**`SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/vocablearning`**
- `jdbc:mysql://` - JDBC protocol
- `mysql` - Service name (Docker DNS resolves this)
- `3306` - MySQL port
- `vocablearning` - Database name

**`SPRING_DATASOURCE_USERNAME: root`**
- MySQL username

**`SPRING_DATASOURCE_PASSWORD: Samsung@11`**
- MySQL password (matches MYSQL_ROOT_PASSWORD)

**`SPRING_AI_OLLAMA_BASE_URL: http://ollama:11434`**
- `ollama` - Service name
- `11434` - Ollama port

---

#### **`depends_on:`**
```yaml
depends_on:
  mysql:
    condition: service_healthy
  ollama:
    condition: service_started
```

**What:** Controls startup order

**Why needed:**
- App needs MySQL to be ready
- App needs Ollama to be running
- Prevents connection errors

**How it works:**
```
docker-compose up
    ↓
1. Start MySQL container
    ↓
2. Wait for MySQL health check to pass
    ↓
3. Start Ollama container
    ↓
4. Wait for Ollama to start (no health check)
    ↓
5. Start App container
```

**Conditions:**

**`service_healthy`** (MySQL)
- Waits for health check to pass
- MySQL must respond to `mysqladmin ping`
- Ensures database is ready

**`service_started`** (Ollama)
- Waits for container to start
- Doesn't check if Ollama is fully ready
- Just ensures process is running

**Startup sequence visualization:**
```
Time →

MySQL:     [Starting...] [Health checks...] [✅ Healthy] [Running]
Ollama:    [Starting...] [✅ Started] [Running]
App:       [Waiting...........................] [✅ Starting] [Running]
                                                      ↑
                                    Starts only after dependencies ready
```

---

## 📦 Volumes Section

```yaml
volumes:
  mysql-data:
  ollama-data:
```

**What:** Declares named volumes used by services

**Why needed:**
- Services reference these volumes
- Docker creates them if they don't exist
- Persists data across container restarts

**Full volume names:**
- `vocablearning_mysql-data` (prefixed with project name)
- `vocablearning_ollama-data`

**What's stored:**
- `mysql-data` → Database tables, user data
- `ollama-data` → AI models (4GB+)

---

## 🏗️ Complete Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                    docker-compose.yml                            │
│                                                                  │
│  Defines 3 services + 2 volumes + 1 network (implicit)          │
└────────────────────────┬─────────────────────────────────────────┘
                         │
                         ↓ docker-compose up
┌─────────────────────────────────────────────────────────────────┐
│                     Docker Compose Network                       │
│                  (vocablearning_default)                         │
│                                                                  │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │  MySQL Container (vocablearning-mysql)                     │ │
│  │  • Image: mysql:8.0                                        │ │
│  │  • Port: 3306                                              │ │
│  │  • Volume: mysql-data → /var/lib/mysql                     │ │
│  │  • Health check: mysqladmin ping                           │ │
│  │  • Environment: MYSQL_ROOT_PASSWORD, MYSQL_DATABASE        │ │
│  └────────────────────────────────────────────────────────────┘ │
│                         ↑                                        │
│                         │ Waits for healthy                      │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │  Ollama Container (vocablearning-ollama)                   │ │
│  │  • Image: ollama/ollama:latest                             │ │
│  │  • Port: 11434                                             │ │
│  │  • Volume: ollama-data → /root/.ollama                     │ │
│  └────────────────────────────────────────────────────────────┘ │
│                         ↑                                        │
│                         │ Waits for started                      │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │  App Container (vocablearning-app)                         │ │
│  │  • Build: Dockerfile in current directory                  │ │
│  │  • Port: 8080                                              │ │
│  │  • Environment: Database & Ollama URLs                     │ │
│  │  • Depends on: mysql (healthy), ollama (started)           │ │
│  └────────────────────────────────────────────────────────────┘ │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
                         ↓
┌─────────────────────────────────────────────────────────────────┐
│                    Docker Volumes                                │
│                                                                  │
│  • vocablearning_mysql-data  (Database persistence)             │
│  • vocablearning_ollama-data (AI models persistence)            │
└─────────────────────────────────────────────────────────────────┘
```

---

## 🔄 Docker Compose Lifecycle

### **1. Starting Services:**
```bash
docker-compose up
```

**What happens:**
```
1. Read docker-compose.yml
2. Create network: vocablearning_default
3. Create volumes: mysql-data, ollama-data
4. Pull images: mysql:8.0, ollama/ollama:latest
5. Build app image from Dockerfile
6. Start MySQL container
7. Run MySQL health checks
8. Start Ollama container
9. Start App container (after dependencies ready)
10. Attach to container logs
```

### **2. Stopping Services:**
```bash
docker-compose down
```

**What happens:**
```
1. Stop all containers gracefully (SIGTERM)
2. Remove containers
3. Remove network
4. Keep volumes (data persists!)
```

### **3. Complete Cleanup:**
```bash
docker-compose down -v
```

**What happens:**
```
1. Stop all containers
2. Remove containers
3. Remove network
4. Remove volumes (⚠️ DATA LOST!)
```

---

## 🎯 Docker Compose Commands

### **Basic Commands:**

```bash
# Start services (foreground)
docker-compose up

# Start services (background/detached)
docker-compose up -d

# Stop services (keeps containers)
docker-compose stop

# Stop and remove containers
docker-compose down

# Stop and remove containers + volumes
docker-compose down -v

# Rebuild images and start
docker-compose up --build

# View logs
docker-compose logs

# View logs for specific service
docker-compose logs app

# Follow logs (live)
docker-compose logs -f

# List running services
docker-compose ps

# Execute command in service
docker-compose exec app bash

# Restart specific service
docker-compose restart app

# View resource usage
docker-compose top
```

### **Advanced Commands:**

```bash
# Scale service (run multiple instances)
docker-compose up --scale app=3

# Validate docker-compose.yml
docker-compose config

# Pull latest images
docker-compose pull

# Build without cache
docker-compose build --no-cache

# Remove stopped containers
docker-compose rm

# Pause services
docker-compose pause

# Unpause services
docker-compose unpause
```

---

## 🌐 Docker Networking

### **Implicit Network:**

Docker Compose automatically creates a network named `{project}_default`

**Your network:** `vocablearning_default`

**How services communicate:**
```
App container:
  - Can reach MySQL at: mysql:3306
  - Can reach Ollama at: ollama:11434

Docker DNS resolves service names to container IPs
```

**Network visualization:**
```
┌─────────────────────────────────────────┐
│   vocablearning_default network         │
│                                         │
│   mysql       → 172.18.0.2:3306        │
│   ollama      → 172.18.0.3:11434       │
│   app         → 172.18.0.4:8080        │
│                                         │
│   DNS: service_name → IP address        │
└─────────────────────────────────────────┘
```

**Inspect network:**
```bash
docker network ls
docker network inspect vocablearning_default
```

---

## 📊 Environment Variables Priority

Spring Boot reads configuration in this order (highest to lowest priority):

```
1. Environment variables (docker-compose.yml)  ← HIGHEST
   SPRING_DATASOURCE_URL=jdbc:mysql://mysql:3306/vocablearning

2. application.properties
   spring.datasource.url=jdbc:mysql://localhost:3306/vocablearning

3. Default values in code
```

**Example:**
```yaml
# docker-compose.yml
environment:
  SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/vocablearning
```

**Overrides:**
```properties
# application.properties
spring.datasource.url=jdbc:mysql://localhost:3306/vocablearning
```

**Result:** App uses `mysql:3306` (from environment variable)

---

## 🔒 Security Best Practices

### **❌ Current Issues:**

```yaml
environment:
  MYSQL_ROOT_PASSWORD: Samsung@11    # ❌ Hardcoded password
  SPRING_DATASOURCE_PASSWORD: Samsung@11    # ❌ Visible in file
```

### **✅ Better Approach:**

**Use `.env` file:**

Create `.env` file:
```env
MYSQL_ROOT_PASSWORD=Samsung@11
DB_PASSWORD=Samsung@11
```

Update `docker-compose.yml`:
```yaml
environment:
  MYSQL_ROOT_PASSWORD: ${MYSQL_ROOT_PASSWORD}
  SPRING_DATASOURCE_PASSWORD: ${DB_PASSWORD}
```

Add to `.gitignore`:
```
.env
```

---

## 💡 Summary

### **What docker-compose.yml Does:**

1. **Defines 3 services:**
   - MySQL (database)
   - Ollama (AI)
   - App (Spring Boot)

2. **Manages dependencies:**
   - App waits for MySQL to be healthy
   - App waits for Ollama to start

3. **Handles networking:**
   - Creates private network
   - Services communicate by name

4. **Persists data:**
   - MySQL data in mysql-data volume
   - AI models in ollama-data volume

5. **Configures environment:**
   - Overrides application.properties
   - Sets database credentials
   - Sets service URLs

### **Key Benefits:**

- ✅ One command to start everything
- ✅ Reproducible environment
- ✅ Easy to share with team
- ✅ Version controlled configuration
- ✅ Automatic dependency management
- ✅ Built-in networking
- ✅ Data persistence

### **Your Configuration:**

```
3 Services + 2 Volumes + 1 Network = Complete Application
```

All managed by one file: `docker-compose.yml` 🚀

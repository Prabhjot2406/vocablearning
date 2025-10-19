# 📦 Docker Volumes - Complete Guide

## 🎯 What is a Docker Volume?

A **Docker Volume** is a persistent storage mechanism that allows data to survive even after containers are stopped or deleted.

### **The Problem Without Volumes:**

```
┌─────────────────────────┐
│   MySQL Container       │
│                         │
│   Database Data         │  ← Stored inside container
│   /var/lib/mysql        │
└─────────────────────────┘
         ↓
    docker stop mysql
         ↓
┌─────────────────────────┐
│   ❌ ALL DATA LOST!     │  ← Container filesystem is temporary
└─────────────────────────┘
```

### **The Solution With Volumes:**

```
┌─────────────────────────┐
│   MySQL Container       │
│                         │
│   /var/lib/mysql   ─────┼──→  ┌──────────────────┐
│                         │     │  Docker Volume   │
└─────────────────────────┘     │  (mysql-data)    │
         ↓                      │  ✅ Persists!    │
    docker stop mysql           └──────────────────┘
         ↓
┌─────────────────────────┐
│   New MySQL Container   │
│                         │
│   /var/lib/mysql   ─────┼──→  Same volume!
│                         │     Data still there!
└─────────────────────────┘
```

---

## 📚 Types of Docker Volumes

### **1. Named Volumes** (RECOMMENDED) ⭐

**What:** Docker-managed volumes with a specific name

**Syntax:**
```yaml
volumes:
  - volume-name:/container/path
```

**Example from your docker-compose.yml:**
```yaml
services:
  mysql:
    volumes:
      - mysql-data:/var/lib/mysql    # Named volume

volumes:
  mysql-data:    # Volume declaration
```

**Characteristics:**
- ✅ Managed by Docker
- ✅ Easy to backup and restore
- ✅ Portable across systems
- ✅ Best for production
- ✅ Survives container deletion

**Storage Location:**
- **Mac:** `/var/lib/docker/volumes/`
- **Linux:** `/var/lib/docker/volumes/`
- **Windows:** `C:\ProgramData\Docker\volumes\`

**Commands:**
```bash
# List volumes
docker volume ls

# Inspect volume
docker volume inspect mysql-data

# Remove volume
docker volume rm mysql-data

# Remove all unused volumes
docker volume prune
```

---

### **2. Bind Mounts** (Host Directory Mapping)

**What:** Maps a specific host directory to container directory

**Syntax:**
```yaml
volumes:
  - /host/path:/container/path
  - ./relative/path:/container/path
```

**Example:**
```yaml
services:
  app:
    volumes:
      - ./src:/app/src                    # Relative path
      - /Users/temp/logs:/app/logs        # Absolute path
```

**Characteristics:**
- ✅ Direct access to files from host
- ✅ Good for development (live code reload)
- ✅ Easy to edit files with host tools
- ❌ Not portable (path must exist on host)
- ❌ Permission issues on Linux

**Use Cases:**
- Development environments
- Configuration files
- Log files you want to access easily
- Source code hot-reloading

**Example - Development Setup:**
```yaml
services:
  app:
    volumes:
      - ./src:/app/src              # Live code changes
      - ./logs:/app/logs            # Easy log access
      - ./config:/app/config        # Config management
```

---

### **3. Anonymous Volumes**

**What:** Docker-managed volumes without a name (auto-generated ID)

**Syntax:**
```yaml
volumes:
  - /container/path    # No name specified
```

**Example:**
```yaml
services:
  app:
    volumes:
      - /app/data    # Anonymous volume
```

**Characteristics:**
- ✅ Automatic creation
- ❌ Hard to identify (random ID)
- ❌ Hard to reuse
- ❌ Not recommended for production

**Generated name example:**
```
a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6q7r8s9t0
```

---

### **4. tmpfs Mounts** (In-Memory Storage)

**What:** Stores data in host memory (RAM), not on disk

**Syntax:**
```yaml
services:
  app:
    tmpfs:
      - /app/temp
```

**Characteristics:**
- ✅ Very fast (RAM speed)
- ✅ Secure (no disk traces)
- ❌ Lost on container stop
- ❌ Limited by RAM size

**Use Cases:**
- Temporary files
- Cache data
- Sensitive data (passwords, tokens)

---

## 🏗️ Volume Types Comparison

| Feature | Named Volume | Bind Mount | Anonymous Volume | tmpfs |
|---------|-------------|------------|------------------|-------|
| **Managed by Docker** | ✅ Yes | ❌ No | ✅ Yes | ❌ No |
| **Portable** | ✅ Yes | ❌ No | ✅ Yes | ❌ No |
| **Easy to backup** | ✅ Yes | ⚠️ Manual | ❌ Hard | ❌ No |
| **Survives container deletion** | ✅ Yes | ✅ Yes | ❌ No | ❌ No |
| **Performance** | Fast | Fast | Fast | Fastest |
| **Host access** | ❌ Hard | ✅ Easy | ❌ Hard | ❌ No |
| **Production use** | ✅ Best | ⚠️ Careful | ❌ No | ⚠️ Specific |

---

## 🎯 Your Docker Compose Configuration

### **What You're Using:**

```yaml
services:
  mysql:
    volumes:
      - mysql-data:/var/lib/mysql    # Named Volume (Type 1)

  ollama:
    volumes:
      - ollama-data:/root/.ollama    # Named Volume (Type 1)

volumes:
  mysql-data:      # Volume declaration
  ollama-data:     # Volume declaration
```

**Type Used:** **Named Volumes** ⭐

**Why This is Perfect:**
- ✅ Database data persists across restarts
- ✅ AI models persist (don't re-download)
- ✅ Easy to backup
- ✅ Production-ready
- ✅ Portable to other machines

---

## 🏛️ Architecture Diagram

### **Your Application Architecture with Volumes:**

```
┌─────────────────────────────────────────────────────────────────┐
│                        Docker Host (Your Mac)                    │
│                                                                  │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │              Docker Compose Network                        │ │
│  │                                                            │ │
│  │  ┌──────────────────┐                                     │ │
│  │  │  MySQL Container │                                     │ │
│  │  │  Port: 3306      │                                     │ │
│  │  │                  │                                     │ │
│  │  │  /var/lib/mysql ─┼─────┐                              │ │
│  │  └──────────────────┘     │                              │ │
│  │                            │                              │ │
│  │                            ↓                              │ │
│  │                   ┌─────────────────┐                    │ │
│  │                   │  Named Volume   │                    │ │
│  │                   │  "mysql-data"   │                    │ │
│  │                   │                 │                    │ │
│  │                   │  ✅ Persists    │                    │ │
│  │                   │  Database Data  │                    │ │
│  │                   └─────────────────┘                    │ │
│  │                                                            │ │
│  │  ┌──────────────────┐                                     │ │
│  │  │ Ollama Container │                                     │ │
│  │  │  Port: 11434     │                                     │ │
│  │  │                  │                                     │ │
│  │  │  /root/.ollama  ─┼─────┐                              │ │
│  │  └──────────────────┘     │                              │ │
│  │                            │                              │ │
│  │                            ↓                              │ │
│  │                   ┌─────────────────┐                    │ │
│  │                   │  Named Volume   │                    │ │
│  │                   │  "ollama-data"  │                    │ │
│  │                   │                 │                    │ │
│  │                   │  ✅ Persists    │                    │ │
│  │                   │  AI Models      │                    │ │
│  │                   └─────────────────┘                    │ │
│  │                                                            │ │
│  │  ┌──────────────────┐                                     │ │
│  │  │  App Container   │                                     │ │
│  │  │  Port: 8080      │                                     │ │
│  │  │                  │                                     │ │
│  │  │  (No volumes)    │  ← Stateless, no data to persist   │ │
│  │  └──────────────────┘                                     │ │
│  │                                                            │ │
│  └────────────────────────────────────────────────────────────┘ │
│                                                                  │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │           Docker Volume Storage Location                   │ │
│  │           /var/lib/docker/volumes/                         │ │
│  │                                                            │ │
│  │  ├── vocablearning_mysql-data/                            │ │
│  │  │   └── _data/                                           │ │
│  │  │       ├── ibdata1                                      │ │
│  │  │       ├── mysql/                                       │ │
│  │  │       └── vocablearning/  ← Your database tables       │ │
│  │  │                                                         │ │
│  │  └── vocablearning_ollama-data/                           │ │
│  │      └── _data/                                           │ │
│  │          └── models/                                      │ │
│  │              └── llama3.2/  ← AI model files (4GB+)       │ │
│  └────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────┘
```

---

## 🔄 Volume Lifecycle

### **Scenario 1: First Time Startup**

```
1. docker-compose up
        ↓
2. Docker creates volumes:
   - vocablearning_mysql-data
   - vocablearning_ollama-data
        ↓
3. Containers start and mount volumes
        ↓
4. MySQL initializes database in volume
        ↓
5. Ollama downloads models to volume
        ↓
✅ Data stored in volumes
```

### **Scenario 2: Container Restart**

```
1. docker-compose down
        ↓
2. Containers stopped and removed
   ❌ Container filesystem deleted
   ✅ Volumes remain intact
        ↓
3. docker-compose up
        ↓
4. New containers created
        ↓
5. Volumes mounted to new containers
        ↓
✅ All data still there!
   - Database tables preserved
   - AI models preserved
```

### **Scenario 3: Complete Cleanup**

```
1. docker-compose down -v
        ↓
2. Containers removed
        ↓
3. Volumes deleted
        ↓
❌ All data lost
   - Database tables gone
   - AI models gone
        ↓
Next startup = Fresh installation
```

---

## 📊 Volume Data Flow

### **MySQL Volume:**

```
┌─────────────────────────────────────────────────────────┐
│  Spring Boot App                                        │
│                                                         │
│  wordRepository.save(wordEntity)                        │
└────────────────────┬────────────────────────────────────┘
                     │ SQL INSERT
                     ↓
┌─────────────────────────────────────────────────────────┐
│  MySQL Container                                        │
│                                                         │
│  MySQL Server Process                                   │
│  Receives SQL command                                   │
└────────────────────┬────────────────────────────────────┘
                     │ Write to disk
                     ↓
┌─────────────────────────────────────────────────────────┐
│  Container Filesystem                                   │
│  /var/lib/mysql/                                        │
└────────────────────┬────────────────────────────────────┘
                     │ Mounted to
                     ↓
┌─────────────────────────────────────────────────────────┐
│  Docker Volume: mysql-data                              │
│  /var/lib/docker/volumes/vocablearning_mysql-data/      │
│                                                         │
│  ✅ Data persists here!                                 │
│  - vocablearning database                               │
│  - words_db table                                       │
│  - All your vocabulary words                            │
└─────────────────────────────────────────────────────────┘
```

### **Ollama Volume:**

```
┌─────────────────────────────────────────────────────────┐
│  Spring Boot App                                        │
│                                                         │
│  aiService.generateWordDetails("Ephemeral")             │
└────────────────────┬────────────────────────────────────┘
                     │ HTTP Request
                     ↓
┌─────────────────────────────────────────────────────────┐
│  Ollama Container                                       │
│                                                         │
│  Ollama Server Process                                  │
│  Loads model from disk                                  │
└────────────────────┬────────────────────────────────────┘
                     │ Read model files
                     ↓
┌─────────────────────────────────────────────────────────┐
│  Container Filesystem                                   │
│  /root/.ollama/                                         │
└────────────────────┬────────────────────────────────────┘
                     │ Mounted to
                     ↓
┌─────────────────────────────────────────────────────────┐
│  Docker Volume: ollama-data                             │
│  /var/lib/docker/volumes/vocablearning_ollama-data/     │
│                                                         │
│  ✅ AI models persist here!                             │
│  - llama3.2 model (4GB+)                                │
│  - Model configuration                                  │
│  - Model cache                                          │
└─────────────────────────────────────────────────────────┘
```

---

## 🛠️ Volume Management Commands

### **Inspect Your Volumes:**

```bash
# List all volumes
docker volume ls

# Output:
# DRIVER    VOLUME NAME
# local     vocablearning_mysql-data
# local     vocablearning_ollama-data

# Inspect specific volume
docker volume inspect vocablearning_mysql-data

# Output shows:
# - Mount point on host
# - Creation date
# - Driver type
# - Size
```

### **Backup Volume Data:**

```bash
# Backup MySQL data
docker run --rm \
  -v vocablearning_mysql-data:/data \
  -v $(pwd):/backup \
  ubuntu tar czf /backup/mysql-backup.tar.gz /data

# Backup Ollama data
docker run --rm \
  -v vocablearning_ollama-data:/data \
  -v $(pwd):/backup \
  ubuntu tar czf /backup/ollama-backup.tar.gz /data
```

### **Restore Volume Data:**

```bash
# Restore MySQL data
docker run --rm \
  -v vocablearning_mysql-data:/data \
  -v $(pwd):/backup \
  ubuntu tar xzf /backup/mysql-backup.tar.gz -C /

# Restore Ollama data
docker run --rm \
  -v vocablearning_ollama-data:/data \
  -v $(pwd):/backup \
  ubuntu tar xzf /backup/ollama-backup.tar.gz -C /
```

### **Clean Up Volumes:**

```bash
# Remove specific volume (must stop containers first)
docker-compose down
docker volume rm vocablearning_mysql-data

# Remove all unused volumes
docker volume prune

# Remove everything including volumes
docker-compose down -v
```

---

## 💡 Best Practices

### ✅ DO:

1. **Use named volumes for databases**
   ```yaml
   volumes:
     - mysql-data:/var/lib/mysql
   ```

2. **Use bind mounts for development**
   ```yaml
   volumes:
     - ./src:/app/src    # Live code reload
   ```

3. **Backup important volumes regularly**
   ```bash
   docker volume inspect mysql-data
   # Copy data to safe location
   ```

4. **Use volume labels for organization**
   ```yaml
   volumes:
     mysql-data:
       labels:
         project: "vocablearning"
         type: "database"
   ```

### ❌ DON'T:

1. **Don't use anonymous volumes in production**
   ```yaml
   volumes:
     - /var/lib/mysql    # Hard to manage
   ```

2. **Don't store volumes on network drives** (slow performance)

3. **Don't manually edit volume data** (use container tools)

4. **Don't forget to backup before `docker-compose down -v`**

---

## 🎓 Summary

### **What Type You're Using:**
**Named Volumes** - The best choice for your use case!

### **Why Named Volumes:**
- ✅ Database data persists across restarts
- ✅ AI models don't need re-downloading
- ✅ Easy to backup and restore
- ✅ Production-ready
- ✅ Docker manages everything

### **Your Configuration:**
```yaml
volumes:
  mysql-data:      # Stores: Database tables, user data
  ollama-data:     # Stores: AI models (4GB+), model cache
```

### **Key Takeaway:**
Volumes are the bridge between temporary container storage and permanent data persistence. Without volumes, all your data would be lost every time you restart containers! 🚀

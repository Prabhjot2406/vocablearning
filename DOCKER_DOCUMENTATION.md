# 🐳 Dockerfile Documentation

## 📋 Overview

This Dockerfile uses a **multi-stage build** strategy to create an optimized Spring Boot application image. It separates the build environment from the runtime environment, resulting in a smaller final image.

---

## 🏗️ Build Architecture

```
┌─────────────────────────────────────────────────────────┐
│                    STAGE 1: BUILD                        │
│         maven:3.9-eclipse-temurin-17 (700 MB)           │
│                                                          │
│  1. Download dependencies (cached layer)                │
│  2. Compile source code                                 │
│  3. Package JAR file                                    │
│                                                          │
│  Output: vocablearning-0.0.1-SNAPSHOT.jar (~50 MB)     │
└─────────────────────────────────────────────────────────┘
                            ↓
                    (Discard build tools)
                            ↓
┌─────────────────────────────────────────────────────────┐
│                   STAGE 2: RUNTIME                       │
│            openjdk:17-jdk-slim (400 MB)                 │
│                                                          │
│  1. Copy JAR from build stage                           │
│  2. Set entrypoint                                      │
│                                                          │
│  Final Image: ~450 MB                                   │
└─────────────────────────────────────────────────────────┘
```

---

## 📦 Base Images

### **Stage 1: Build Stage**
```dockerfile
FROM maven:3.9-eclipse-temurin-17 AS build
```

| Property | Value |
|----------|-------|
| **Image** | `maven:3.9-eclipse-temurin-17` |
| **Size** | ~700 MB |
| **Purpose** | Build environment with Maven + JDK |
| **Includes** | Maven 3.9, Java 17 JDK, build tools |
| **Discarded** | ✅ Yes (multi-stage build) |

**Why this image?**
- Contains Maven for dependency management
- Includes full JDK for compilation
- Official Eclipse Temurin Java distribution
- Not included in final image (discarded after build)

---

### **Stage 2: Runtime Stage**
```dockerfile
FROM openjdk:17-jdk-slim
```

| Property | Value |
|----------|-------|
| **Image** | `openjdk:17-jdk-slim` |
| **Size** | ~400 MB |
| **Purpose** | Runtime environment for Spring Boot |
| **Includes** | Java 17 JDK (slim variant) |
| **Final Image** | ✅ Yes (this becomes the final image) |

**Why this image?**
- Slim variant (smaller than full JDK)
- Sufficient for running Spring Boot applications
- Debian-based for compatibility

---

## ⏱️ Deployment Time Breakdown

### **First Build (Cold Start - No Cache)**

| Step | Command | Time | Cached? |
|------|---------|------|---------|
| Pull build image | `FROM maven:3.9-eclipse-temurin-17` | 3-5 min | ❌ |
| Copy pom.xml | `COPY pom.xml .` | 1 sec | ❌ |
| Download dependencies | `RUN mvn dependency:go-offline` | 2-3 min | ❌ |
| Copy source code | `COPY src ./src` | 2 sec | ❌ |
| Build JAR | `RUN mvn clean package -DskipTests` | 1-2 min | ❌ |
| Pull runtime image | `FROM openjdk:17-jdk-slim` | 2-3 min | ❌ |
| Copy JAR | `COPY --from=build ...` | 1 sec | ❌ |
| **TOTAL** | | **8-14 minutes** | |

---

### **Rebuild (Warm Start - With Cache)**

#### **Scenario A: Only Source Code Changed**

| Step | Command | Time | Cached? |
|------|---------|------|---------|
| Pull build image | `FROM maven:3.9-eclipse-temurin-17` | 0 sec | ✅ Cached |
| Copy pom.xml | `COPY pom.xml .` | 0 sec | ✅ Cached |
| Download dependencies | `RUN mvn dependency:go-offline` | 0 sec | ✅ **Cached** |
| Copy source code | `COPY src ./src` | 2 sec | ❌ Changed |
| Build JAR | `RUN mvn clean package -DskipTests` | 30-60 sec | ❌ Rebuild |
| Pull runtime image | `FROM openjdk:17-jdk-slim` | 0 sec | ✅ Cached |
| Copy JAR | `COPY --from=build ...` | 1 sec | ❌ New JAR |
| **TOTAL** | | **~1 minute** | |

**Key optimization:** Dependencies are cached, only recompiles changed code.

---

#### **Scenario B: Dependencies Changed (pom.xml modified)**

| Step | Command | Time | Cached? |
|------|---------|------|---------|
| Pull build image | `FROM maven:3.9-eclipse-temurin-17` | 0 sec | ✅ Cached |
| Copy pom.xml | `COPY pom.xml .` | 1 sec | ❌ Changed |
| Download dependencies | `RUN mvn dependency:go-offline` | 2-3 min | ❌ Re-download |
| Copy source code | `COPY src ./src` | 2 sec | ❌ |
| Build JAR | `RUN mvn clean package -DskipTests` | 1-2 min | ❌ |
| Pull runtime image | `FROM openjdk:17-jdk-slim` | 0 sec | ✅ Cached |
| Copy JAR | `COPY --from=build ...` | 1 sec | ❌ |
| **TOTAL** | | **3-5 minutes** | |

---

## 🎯 Layer Caching Strategy

### **How Docker Caching Works**

Docker caches each layer (instruction) in the Dockerfile. If a layer hasn't changed, Docker reuses the cached version.

```
Layer 1: FROM maven:3.9-eclipse-temurin-17        ← Cached (base image)
Layer 2: WORKDIR /app                             ← Cached (no change)
Layer 3: COPY pom.xml .                           ← Cached if pom.xml unchanged
Layer 4: RUN mvn dependency:go-offline            ← Cached if Layer 3 cached
Layer 5: COPY src ./src                           ← Invalidated when code changes
Layer 6: RUN mvn clean package                    ← Rebuilt (Layer 5 changed)
Layer 7: FROM openjdk:17-jdk-slim                 ← Cached (base image)
Layer 8: COPY --from=build .../app.jar            ← Rebuilt (new JAR)
```

---

### **Optimization: Dependencies Before Source Code**

**Why this order matters:**

```dockerfile
# ✅ CORRECT ORDER (Optimized)
COPY pom.xml .                    # Changes rarely
RUN mvn dependency:go-offline     # Cached most of the time
COPY src ./src                    # Changes frequently
RUN mvn clean package             # Only recompiles code
```

```dockerfile
# ❌ WRONG ORDER (Not optimized)
COPY . .                          # Copies everything
RUN mvn clean package             # Re-downloads dependencies every time
```

**Impact:**
- **Correct order:** Rebuild in ~1 minute (code changes)
- **Wrong order:** Rebuild in ~5 minutes (re-downloads dependencies)

---

## 📊 Image Size Breakdown

### **Current Configuration**

| Component | Size | Percentage |
|-----------|------|------------|
| Base OS (Debian slim) | ~100 MB | 22% |
| Java 17 JDK | ~300 MB | 67% |
| Application JAR | ~50 MB | 11% |
| **Total Final Image** | **~450 MB** | **100%** |

**Note:** Build stage (~700 MB) is discarded and not included in final image.

---

### **What's NOT in the Final Image**

Thanks to multi-stage build, these are excluded:

- ❌ Maven (~50 MB)
- ❌ Build tools and compilers (~100 MB)
- ❌ Source code (~5 MB)
- ❌ Maven dependencies cache (~200 MB)
- ❌ Intermediate build artifacts

**Savings:** ~355 MB (44% reduction)

---

## 🚀 Deployment Scenarios

### **Scenario 1: Local Development**

```bash
# First build
docker build -t vocablearning:latest .
# Time: 8-14 minutes

# Code change and rebuild
docker build -t vocablearning:latest .
# Time: ~1 minute (dependencies cached)
```

---

### **Scenario 2: CI/CD Pipeline**

```bash
# Build with BuildKit (faster)
DOCKER_BUILDKIT=1 docker build -t vocablearning:latest .

# Push to registry
docker push yourusername/vocablearning:latest
# Time: 2-4 minutes (uploads ~450 MB)
```

---

### **Scenario 3: Production Deployment**

```bash
# Pull pre-built image (no build needed)
docker pull yourusername/vocablearning:latest
# Time: 45 seconds (downloads ~450 MB on 100 Mbps)

# Start container
docker run -p 8080:8080 vocablearning:latest
# Time: 5-10 seconds
```

---

## 🔧 Build Commands

### **Basic Build**
```bash
docker build -t vocablearning:latest .
```

### **Build with Tag**
```bash
docker build -t vocablearning:v1.0.0 .
```

### **Build with No Cache (Force Rebuild)**
```bash
docker build --no-cache -t vocablearning:latest .
```

### **Build with BuildKit (Faster)**
```bash
DOCKER_BUILDKIT=1 docker build -t vocablearning:latest .
```

### **Check Image Size**
```bash
docker images vocablearning:latest
```

---

## 📈 Performance Metrics

### **Build Performance**

| Metric | First Build | Rebuild (code change) | Rebuild (deps change) |
|--------|-------------|----------------------|----------------------|
| **Time** | 8-14 min | ~1 min | 3-5 min |
| **Cache Hit Rate** | 0% | ~80% | ~40% |
| **Network Usage** | High | Low | Medium |
| **CPU Usage** | High | Medium | High |

---

### **Deployment Performance**

| Metric | Build on Server | Pull from Registry |
|--------|----------------|-------------------|
| **Time** | 8-14 min | 45 sec |
| **Network** | Downloads base images | Downloads final image |
| **Disk I/O** | High (compilation) | Low (just download) |
| **Recommended** | ❌ Development only | ✅ Production |

---

## 🎯 Optimization Opportunities

### **Current: 450 MB, 1 min rebuild**

### **Potential Improvements:**

1. **Switch to Alpine JRE** → 180 MB (60% reduction)
   ```dockerfile
   FROM eclipse-temurin:17-jre-alpine
   ```

2. **Use Distroless** → 120 MB (73% reduction)
   ```dockerfile
   FROM gcr.io/distroless/java17-debian12
   ```

3. **Add .dockerignore** → Faster builds
   ```
   target/
   .git/
   *.md
   ```

4. **Layer optimization** → 30 sec rebuilds
   ```dockerfile
   # Extract JAR layers for better caching
   RUN java -Djarmode=layertools -jar app.jar extract
   ```

See optimization guide for detailed implementation steps.

---

## 📝 Best Practices Applied

✅ **Multi-stage build** - Separates build and runtime  
✅ **Layer caching** - Dependencies copied before source code  
✅ **Slim base image** - Uses `-slim` variant  
✅ **Specific versions** - `maven:3.9` and `openjdk:17` (not `latest`)  
✅ **Single responsibility** - Each stage has clear purpose  
✅ **Minimal layers** - Combines related commands  

---

## 🔍 Troubleshooting

### **Build is slow**
- Check internet connection (downloads dependencies)
- Use `DOCKER_BUILDKIT=1` for faster builds
- Ensure Docker has enough resources (4GB+ RAM)

### **Cache not working**
- Check if `pom.xml` changed (invalidates dependency cache)
- Verify `.dockerignore` exists
- Use `docker build --progress=plain` to see cache hits

### **Image too large**
- Current: 450 MB is reasonable for JDK-based image
- For smaller: Switch to JRE or Distroless (see optimization guide)

---

## 📚 Related Documentation

- [README.md](README.md) - Application architecture and setup
- [docker-compose.yml](docker-compose.yml) - Multi-container orchestration
- Optimization guide - Image size reduction strategies

---

## 📊 Quick Reference

| Metric | Value |
|--------|-------|
| **Final Image Size** | ~450 MB |
| **Build Stage Size** | ~700 MB (discarded) |
| **First Build Time** | 8-14 minutes |
| **Rebuild Time (code change)** | ~1 minute |
| **Deployment Time (pull)** | 45 seconds (100 Mbps) |
| **Container Start Time** | 5-10 seconds |
| **Base Images** | maven:3.9-eclipse-temurin-17, openjdk:17-jdk-slim |

---

**Last Updated:** 2024  
**Dockerfile Version:** 1.0  
**Optimization Level:** Basic (multi-stage build)

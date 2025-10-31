# 🐳 Dockerfile Explained - Complete Guide

This document explains every line of the Dockerfile used to containerize the Vocabulary Learning Spring Boot application.

---

## Table of Contents
1. [Complete Dockerfile](#complete-dockerfile)
2. [Multi-Stage Build Concept](#multi-stage-build-concept)
3. [Stage 1: Build Stage](#stage-1-build-stage)
4. [Stage 2: Runtime Stage](#stage-2-runtime-stage)
5. [Key Concepts Explained](#key-concepts-explained)
6. [Build Process Flow](#build-process-flow)
7. [Docker Layer Caching](#docker-layer-caching)
8. [Common Questions](#common-questions)

---

## Complete Dockerfile

```dockerfile
# Use Maven image with Java 17 to build the application
FROM maven:3.9-eclipse-temurin-17 AS build

# Set working directory
WORKDIR /app

# Copy pom.xml and download dependencies (cached layer)
COPY pom.xml .
RUN mvn dependency:go-offline

# Copy source code
COPY src ./src

# Build the application (Step 2: Build JAR inside Docker)
RUN mvn clean package -DskipTests

# Use Java 17 runtime image for running the application
FROM openjdk:17-jdk-slim

# Set working directory
WORKDIR /app

# Copy the JAR file from build stage
COPY --from=build /app/target/vocablearning-0.0.1-SNAPSHOT.jar app.jar

# Expose port 8080
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
```

---

## Multi-Stage Build Concept

This Dockerfile uses a **multi-stage build** pattern with 2 stages:

```
┌─────────────────────────────────────────────────────────────┐
│                    STAGE 1: BUILD                           │
│  ┌────────────────────────────────────────────────────┐    │
│  │  Maven + Java 17 (Heavy - 700MB+)                  │    │
│  │  - Downloads dependencies                           │    │
│  │  - Compiles source code                             │    │
│  │  - Creates JAR file                                 │    │
│  └────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────┘
                            ↓
                    Copies only JAR
                            ↓
┌─────────────────────────────────────────────────────────────┐
│                    STAGE 2: RUNTIME                         │
│  ┌────────────────────────────────────────────────────┐    │
│  │  Java 17 Runtime Only (Light - 200MB)              │    │
│  │  - Runs the JAR file                                │    │
│  │  - No build tools needed                            │    │
│  └────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────┘
```

**Benefits:**
- **Smaller final image** - Only runtime, no build tools
- **Faster deployment** - Less data to transfer
- **More secure** - Fewer tools = smaller attack surface

---

## Stage 1: Build Stage

### Line 1-2: Base Image Selection

```dockerfile
FROM maven:3.9-eclipse-temurin-17 AS build
```

**Breakdown:**
- `FROM` - Specifies the base image to start from
- `maven:3.9-eclipse-temurin-17` - Official Maven image with Java 17
- `AS build` - Names this stage "build" (for reference later)

**What this image contains:**
- Maven 3.9 (build tool)
- Eclipse Temurin Java 17 (JDK)
- Linux OS
- All necessary build tools

---

### Line 4-5: Set Working Directory

```dockerfile
WORKDIR /app
```

**What it does:**
- Creates `/app` directory inside the container
- Sets it as the current working directory
- All subsequent commands run from this directory

**Container structure after this:**
```
Container:
/
├── app/          ← We are here
├── bin/
├── usr/
└── ...
```

---

### Line 7-9: Copy Dependencies File and Download

```dockerfile
COPY pom.xml .
RUN mvn dependency:go-offline
```

**Line 7: `COPY pom.xml .`**
- Copies `pom.xml` from your local machine to `/app/` in container
- `.` means current directory (`/app`)

**Line 8: `RUN mvn dependency:go-offline`**
- Downloads all Maven dependencies listed in `pom.xml`
- Stores them in Maven's local repository inside the container
- Creates a **cached Docker layer** (important for performance!)

**Why separate from source code?**
```
If only source code changes:
├── pom.xml (unchanged) ✅ Use cached layer
└── src/ (changed) ❌ Re-run from here

Result: Dependencies NOT re-downloaded! 🚀
```

**Container structure after this:**
```
Container:
/app/
├── pom.xml
└── .m2/          ← Maven dependencies cached here
    └── repository/
        └── (all dependencies)
```

---

### Line 11-12: Copy Source Code

```dockerfile
COPY src ./src
```

**What it does:**
- Copies your entire `src/` folder from local machine
- Places it in `/app/src/` inside container

**Container structure after this:**
```
Container:
/app/
├── pom.xml
├── src/
│   ├── main/
│   │   ├── java/
│   │   └── resources/
│   └── test/
└── .m2/
```

---

### Line 14-15: Build the Application

```dockerfile
RUN mvn clean package -DskipTests
```

**Breakdown:**

#### `mvn clean`
- Deletes the `target/` folder
- Removes any old build artifacts
- Ensures a fresh build

#### `package`
- Compiles Java source code (`.java` → `.class`)
- Runs the build lifecycle
- Creates a JAR file in `target/` folder

#### `-DskipTests`
- Skips running unit tests
- Speeds up the build
- Tests should be run separately in CI/CD pipeline

**Why skip tests?**
- ✅ Faster Docker builds
- ✅ Tests may need external dependencies (DB, APIs)
- ✅ Tests already run in CI/CD before building image
- ❌ Don't skip if tests are critical for your workflow

**Container structure after this:**
```
Container:
/app/
├── pom.xml
├── src/
├── target/                                    ← Created by Maven
│   ├── classes/                               (compiled .class files)
│   ├── vocablearning-0.0.1-SNAPSHOT.jar      ← Your application!
│   └── ...
└── .m2/
```

---

## Stage 2: Runtime Stage

### Line 17-18: New Base Image

```dockerfile
FROM openjdk:17-jdk-slim
```

**What happens here:**
- **Starts a completely new image** (previous stage is discarded)
- Uses a lightweight Java 17 runtime image
- No Maven, no build tools - just Java to run the JAR

**Image size comparison:**
- Stage 1 (maven:3.9-eclipse-temurin-17): ~700MB
- Stage 2 (openjdk:17-jdk-slim): ~200MB

**Why a new stage?**
```
Build Stage (700MB):
├── Maven ✅ (needed for building)
├── Build tools ✅ (needed for building)
├── Source code ✅ (needed for building)
└── JAR file ✅ (needed for running)

Runtime Stage (200MB):
├── Maven ❌ (not needed)
├── Build tools ❌ (not needed)
├── Source code ❌ (not needed)
└── JAR file ✅ (only this is needed!)
```

---

### Line 20-21: Set Working Directory

```dockerfile
WORKDIR /app
```

**What it does:**
- Creates `/app` directory in the **new** container
- Sets it as working directory

**Note:** This is a fresh container, so we need to set up the directory again.

---

### Line 23-24: Copy JAR from Build Stage

```dockerfile
COPY --from=build /app/target/vocablearning-0.0.1-SNAPSHOT.jar app.jar
```

**Breakdown:**

#### `COPY --from=build`
- Copies files from the **build stage** (Stage 1)
- Not from your local machine!

#### `/app/target/vocablearning-0.0.1-SNAPSHOT.jar`
- Source: JAR file location in build stage

#### `app.jar`
- Destination: Renamed to `app.jar` in current stage
- Simpler, shorter name

**Why rename?**
- Easier to reference: `app.jar` vs `vocablearning-0.0.1-SNAPSHOT.jar`
- Version-independent: If version changes in `pom.xml`, Dockerfile doesn't need update

**Container structure after this:**
```
Runtime Container:
/app/
└── app.jar    ← Your Spring Boot application (only thing here!)
```

---

### Line 26-27: Expose Port

```dockerfile
EXPOSE 8080
```

**What it does:**
- **Documents** that the container listens on port 8080
- Does NOT actually publish the port
- Informational only (for developers/documentation)

**To actually access the port:**
```bash
docker run -p 8080:8080 myapp
           ↑         ↑
      Host port  Container port
```

---

### Line 29-30: Run the Application

```dockerfile
ENTRYPOINT ["java", "-jar", "app.jar"]
```

**What it does:**
- Defines the command to run when container starts
- Executes: `java -jar app.jar`
- Starts your Spring Boot application

**ENTRYPOINT vs CMD:**
- `ENTRYPOINT` - Main command (cannot be overridden easily)
- `CMD` - Default arguments (can be overridden)

**When container starts:**
```bash
$ docker run myapp
# Executes: java -jar app.jar
# Spring Boot application starts on port 8080
```

---

## Key Concepts Explained

### 1. What is `target/` Folder?

**Definition:**
- Maven's **build output directory**
- Created automatically when you run `mvn package`
- Contains all compiled code and build artifacts

**Contents:**
```
target/
├── classes/                          # Compiled .class files
├── test-classes/                     # Compiled test .class files
├── maven-status/                     # Build metadata
├── vocablearning-0.0.1-SNAPSHOT.jar  # Your application JAR ← This is what you need!
└── other build files...
```

**Why `mvn clean` deletes it?**
- Ensures a fresh build
- Removes old artifacts
- Prevents conflicts

---

### 2. What is `app.jar`?

**Definition:**
- A **renamed copy** of your application JAR file
- Just a simpler, shorter name for convenience

**The Renaming:**
```dockerfile
COPY --from=build /app/target/vocablearning-0.0.1-SNAPSHOT.jar app.jar
                  ↑                                            ↑
            Original long name                          Renamed to app.jar
```

**Benefits:**
1. Simpler to reference in commands
2. Version-independent (no need to update Dockerfile when version changes)
3. Cleaner, more readable

---

### 3. What is `mvn dependency:go-offline`?

**Definition:**
- Maven command that downloads all project dependencies
- Stores them in Maven's local repository
- Prepares for offline builds

**Why use it?**
- **Docker layer caching** - Dependencies are downloaded in a separate layer
- **Faster rebuilds** - If only source code changes, Docker reuses cached dependencies
- **No re-download** - Dependencies aren't downloaded again on every build

**Without this optimization:**
```dockerfile
COPY pom.xml .
COPY src ./src
RUN mvn clean package  # Downloads dependencies every time (slow!)
```

**With this optimization:**
```dockerfile
COPY pom.xml .
RUN mvn dependency:go-offline  # Downloads once, cached ✅
COPY src ./src
RUN mvn clean package  # Uses cached dependencies (fast!)
```

---

### 4. What is `mvn clean package -DskipTests`?

**Breakdown:**

#### `mvn clean`
- Deletes the `target/` folder
- Removes old builds

#### `package`
- Compiles code
- Creates JAR file in `target/` folder

#### `-DskipTests`
- Skips running unit tests

**Why skip tests?**

| Without `-DskipTests` | With `-DskipTests` |
|----------------------|-------------------|
| Compiles code ✅ | Compiles code ✅ |
| Runs all tests ✅ | Skips tests ⏭️ |
| Creates JAR ✅ | Creates JAR ✅ |
| Slower build ⏱️ | Faster build 🚀 |
| May fail if tests need DB ❌ | Won't fail ✅ |

**When to use `-DskipTests`:**
- ✅ Building Docker images (tests run in CI/CD)
- ✅ Tests require external dependencies
- ✅ You want faster builds

**When NOT to use:**
- ❌ Tests are critical before deployment
- ❌ No separate CI/CD pipeline

---

## Build Process Flow

### Complete Build Flow Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                    YOUR LOCAL MACHINE                           │
│                                                                 │
│  vocablearning/                                                 │
│  ├── src/                                                       │
│  ├── pom.xml                                                    │
│  └── Dockerfile                                                 │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
                            ↓
                    docker build -t myapp .
                            ↓
┌─────────────────────────────────────────────────────────────────┐
│                    STAGE 1: BUILD STAGE                         │
│  ┌───────────────────────────────────────────────────────────┐ │
│  │ FROM maven:3.9-eclipse-temurin-17 AS build               │ │
│  │ Base Image: Maven + Java 17 (~700MB)                     │ │
│  └───────────────────────────────────────────────────────────┘ │
│                            ↓                                    │
│  ┌───────────────────────────────────────────────────────────┐ │
│  │ WORKDIR /app                                              │ │
│  │ Creates /app directory                                    │ │
│  └───────────────────────────────────────────────────────────┘ │
│                            ↓                                    │
│  ┌───────────────────────────────────────────────────────────┐ │
│  │ COPY pom.xml .                                            │ │
│  │ Copies dependency file                                    │ │
│  └───────────────────────────────────────────────────────────┘ │
│                            ↓                                    │
│  ┌───────────────────────────────────────────────────────────┐ │
│  │ RUN mvn dependency:go-offline                             │ │
│  │ Downloads all dependencies (CACHED LAYER)                 │ │
│  └───────────────────────────────────────────────────────────┘ │
│                            ↓                                    │
│  ┌───────────────────────────────────────────────────────────┐ │
│  │ COPY src ./src                                            │ │
│  │ Copies source code                                        │ │
│  └───────────────────────────────────────────────────────────┘ │
│                            ↓                                    │
│  ┌───────────────────────────────────────────────────────────┐ │
│  │ RUN mvn clean package -DskipTests                         │ │
│  │ Compiles code → Creates JAR in target/                   │ │
│  └───────────────────────────────────────────────────────────┘ │
│                            ↓                                    │
│  Result: /app/target/vocablearning-0.0.1-SNAPSHOT.jar         │
└─────────────────────────────────────────────────────────────────┘
                            ↓
                    Copies only JAR
                            ↓
┌─────────────────────────────────────────────────────────────────┐
│                    STAGE 2: RUNTIME STAGE                       │
│  ┌───────────────────────────────────────────────────────────┐ │
│  │ FROM openjdk:17-jdk-slim                                  │ │
│  │ Base Image: Java 17 Runtime Only (~200MB)                │ │
│  └───────────────────────────────────────────────────────────┘ │
│                            ↓                                    │
│  ┌───────────────────────────────────────────────────────────┐ │
│  │ WORKDIR /app                                              │ │
│  │ Creates /app directory in new container                  │ │
│  └───────────────────────────────────────────────────────────┘ │
│                            ↓                                    │
│  ┌───────────────────────────────────────────────────────────┐ │
│  │ COPY --from=build /app/target/*.jar app.jar              │ │
│  │ Copies JAR from build stage and renames it               │ │
│  └───────────────────────────────────────────────────────────┘ │
│                            ↓                                    │
│  ┌───────────────────────────────────────────────────────────┐ │
│  │ EXPOSE 8080                                               │ │
│  │ Documents that app listens on port 8080                  │ │
│  └───────────────────────────────────────────────────────────┘ │
│                            ↓                                    │
│  ┌───────────────────────────────────────────────────────────┐ │
│  │ ENTRYPOINT ["java", "-jar", "app.jar"]                   │ │
│  │ Command to run when container starts                     │ │
│  └───────────────────────────────────────────────────────────┘ │
│                            ↓                                    │
│  Result: Lightweight container with only app.jar (~200MB)     │
└─────────────────────────────────────────────────────────────────┘
                            ↓
                    docker run -p 8080:8080 myapp
                            ↓
┌─────────────────────────────────────────────────────────────────┐
│                    RUNNING CONTAINER                            │
│                                                                 │
│  Spring Boot Application Running on Port 8080                  │
│  Access at: http://localhost:8080                              │
└─────────────────────────────────────────────────────────────────┘
```

---

## Docker Layer Caching

### How Docker Caching Works

Docker builds images in **layers**. Each instruction creates a new layer.

```
┌─────────────────────────────────────────┐
│ Layer 5: ENTRYPOINT ["java", "-jar"]   │
├─────────────────────────────────────────┤
│ Layer 4: COPY app.jar                   │
├─────────────────────────────────────────┤
│ Layer 3: WORKDIR /app                   │
├─────────────────────────────────────────┤
│ Layer 2: FROM openjdk:17-jdk-slim      │
├─────────────────────────────────────────┤
│ Layer 1: Base OS                        │
└─────────────────────────────────────────┘
```

**Caching Rules:**
- If a layer hasn't changed, Docker reuses it (cached)
- If a layer changes, Docker rebuilds it and all layers after it

---

### Optimized vs Non-Optimized Dockerfile

#### ❌ **Non-Optimized (Slow):**

```dockerfile
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY . .                          # Copies everything at once
RUN mvn clean package -DskipTests # Downloads dependencies every time
```

**Problem:**
- Any code change → Re-downloads all dependencies
- Slow builds every time

---

#### ✅ **Optimized (Fast):**

```dockerfile
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .                    # Copy dependencies file first
RUN mvn dependency:go-offline     # Download dependencies (CACHED)
COPY src ./src                    # Copy source code after
RUN mvn clean package -DskipTests # Uses cached dependencies
```

**Benefit:**
- Code change → Dependencies layer is cached
- Only source code layer rebuilds
- Much faster!

---

### Caching Example

**First Build:**
```
Step 1: FROM maven... ✅ Downloaded
Step 2: WORKDIR /app ✅ Created
Step 3: COPY pom.xml ✅ Copied
Step 4: RUN mvn dependency:go-offline ✅ Downloaded (5 minutes)
Step 5: COPY src ✅ Copied
Step 6: RUN mvn clean package ✅ Built (2 minutes)

Total: 7+ minutes
```

**Second Build (only code changed):**
```
Step 1: FROM maven... ✅ Using cache
Step 2: WORKDIR /app ✅ Using cache
Step 3: COPY pom.xml ✅ Using cache
Step 4: RUN mvn dependency:go-offline ✅ Using cache (instant!)
Step 5: COPY src ❌ Changed, rebuilding
Step 6: RUN mvn clean package ❌ Rebuilding (2 minutes)

Total: 2 minutes (5 minutes saved!)
```

---

## Common Questions

### Q1: Why use multi-stage builds?

**Answer:**
- **Smaller images** - Final image only contains runtime, not build tools
- **Faster deployments** - Less data to transfer
- **More secure** - Fewer tools = smaller attack surface
- **Cleaner** - Build artifacts don't pollute final image

**Size comparison:**
- Single-stage: ~700MB (includes Maven, build tools, source code)
- Multi-stage: ~200MB (only Java runtime + JAR)

---

### Q2: Can I skip the dependency caching step?

**Answer:**
Yes, but you'll have slower builds:

```dockerfile
# Without caching (slower)
COPY . .
RUN mvn clean package -DskipTests

# With caching (faster)
COPY pom.xml .
RUN mvn dependency:go-offline
COPY src ./src
RUN mvn clean package -DskipTests
```

**Recommendation:** Always use dependency caching for faster builds.

---

### Q3: Should I run tests in Docker build?

**Answer:**
It depends on your workflow:

**Skip tests (`-DskipTests`) when:**
- ✅ Tests run in CI/CD pipeline before building image
- ✅ Tests need external dependencies (database, APIs)
- ✅ You want faster Docker builds

**Run tests (remove `-DskipTests`) when:**
- ✅ No separate CI/CD pipeline
- ✅ Tests are critical before creating image
- ✅ Tests don't need external dependencies

---

### Q4: What's the difference between ENTRYPOINT and CMD?

**Answer:**

**ENTRYPOINT:**
- Main command that always runs
- Cannot be easily overridden
- Use for the primary executable

**CMD:**
- Default arguments to ENTRYPOINT
- Can be overridden when running container
- Use for default parameters

**Example:**
```dockerfile
ENTRYPOINT ["java", "-jar"]
CMD ["app.jar"]

# Run normally:
docker run myapp
# Executes: java -jar app.jar

# Override CMD:
docker run myapp other.jar
# Executes: java -jar other.jar
```

---

### Q5: Why rename JAR to app.jar?

**Answer:**

**Benefits:**
1. **Simpler** - `app.jar` vs `vocablearning-0.0.1-SNAPSHOT.jar`
2. **Version-independent** - No need to update Dockerfile when version changes
3. **Cleaner** - Easier to read and maintain

**Example:**
```dockerfile
# Without renaming (version-specific)
COPY --from=build /app/target/vocablearning-0.0.1-SNAPSHOT.jar vocablearning-0.0.1-SNAPSHOT.jar
ENTRYPOINT ["java", "-jar", "vocablearning-0.0.1-SNAPSHOT.jar"]

# With renaming (version-independent)
COPY --from=build /app/target/vocablearning-0.0.1-SNAPSHOT.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
```

---

### Q6: How do I build and run this Dockerfile?

**Build the image:**
```bash
docker build -t vocablearning:latest .
```

**Run the container:**
```bash
docker run -p 8080:8080 vocablearning:latest
```

**Access the application:**
```
http://localhost:8080
```

---

### Q7: What if I need to pass environment variables?

**Add to Dockerfile:**
```dockerfile
ENV SPRING_PROFILES_ACTIVE=prod
ENV DATABASE_URL=jdbc:mysql://localhost:3306/mydb
```

**Or pass at runtime:**
```bash
docker run -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e DATABASE_URL=jdbc:mysql://host:3306/db \
  vocablearning:latest
```

---

## Summary

### Key Takeaways

1. **Multi-stage builds** reduce final image size significantly
2. **Dependency caching** speeds up rebuilds dramatically
3. **Skipping tests** in Docker is common (run them in CI/CD)
4. **Layer ordering** matters for caching efficiency
5. **Renaming JAR** makes Dockerfile version-independent

### Best Practices

✅ Use multi-stage builds
✅ Cache dependencies separately
✅ Copy files in optimal order (least changing first)
✅ Use specific base image versions
✅ Keep final image small
✅ Document exposed ports
✅ Use ENTRYPOINT for main command

### Build Commands

```bash
# Build image
docker build -t vocablearning:latest .

# Run container
docker run -p 8080:8080 vocablearning:latest

# Run with environment variables
docker run -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  vocablearning:latest

# Run in background
docker run -d -p 8080:8080 vocablearning:latest

# View logs
docker logs <container-id>

# Stop container
docker stop <container-id>
```

---

**Created:** 2024
**Purpose:** Complete explanation of Dockerfile for Vocabulary Learning Application
**Author:** Docker Learning Documentation

# 📚 Vocabulary Learning Application

A Spring Boot web application that helps users build their vocabulary by storing words with meanings and example sentences. The application features AI-powered word definition and sentence generation using Ollama's local LLM.

## 🚀 Technologies Used

### Backend Framework
- **Spring Boot 3.5.6** - Main application framework
- **Spring Web** - RESTful web services and MVC
- **Spring Data JPA** - Database abstraction and ORM
- **Spring AI (Ollama)** - AI integration for word generation

### Database
- **MySQL** - Primary production database
- **H2** - In-memory database (available for testing)

### Frontend
- **Thymeleaf** - Server-side template engine
- **Bootstrap 5.3.2** - CSS framework for responsive UI
- **Vanilla JavaScript** - Client-side interactivity

### Build Tool
- **Maven** - Dependency management and build automation

### Additional Libraries
- **Lombok** - Reduces boilerplate code (getters, setters, etc.)
- **Ollama (llama3.2)** - Local LLM for AI-powered content generation

---

## 🏗️ Project Architecture

The application follows the **MVC (Model-View-Controller)** pattern with a layered architecture:

```
┌─────────────────────────────────────────────────────────┐
│                    Presentation Layer                    │
│              (HTML Pages + JavaScript)                   │
└────────────────────┬────────────────────────────────────┘
                     │ HTTP Requests
┌────────────────────▼────────────────────────────────────┐
│                   Controller Layer                       │
│                  (HomeController)                        │
└────────────────────┬────────────────────────────────────┘
                     │ Business Logic Calls
┌────────────────────▼────────────────────────────────────┐
│                    Service Layer                         │
│         (WordService + AIService)                        │
└────────────┬───────────────────────┬────────────────────┘
             │                       │
             │ Database Ops          │ AI Calls
┌────────────▼────────────┐  ┌──────▼─────────────────────┐
│   Repository Layer      │  │   External AI Service      │
│   (WordRepository)      │  │   (Ollama LLM)             │
└────────────┬────────────┘  └────────────────────────────┘
             │
┌────────────▼────────────┐
│    Database Layer       │
│    (MySQL - words_db)   │
└─────────────────────────┘
```

---

## 📦 Package Structure & Class Responsibilities

### 1. **Model Package** (`com.example.vocablearning.Model`)

#### `Word.java`
- **Purpose**: Data Transfer Object (DTO) for transferring word data between layers
- **Responsibility**: 
  - Represents word data in the application layer
  - Used for API requests/responses and form binding
  - Contains: id, word, meaning, sentence
- **Why it exists**: Separates external API representation from internal database structure

---

### 2. **Entity Package** (`com.example.vocablearning.Entity`)

#### `WordEntity.java`
- **Purpose**: JPA Entity that maps to the database table
- **Responsibility**:
  - Represents the `words_db` table structure in MySQL
  - Annotated with `@Entity`, `@Table`, `@Id` for ORM mapping
  - Uses Lombok's `@Data` to auto-generate getters/setters
- **Why it exists**: JPA requires entity classes to interact with database tables
- **Key Annotations**:
  - `@Entity` - Marks this as a JPA entity
  - `@Table(name = "words_db")` - Maps to specific table
  - `@Id` - Marks primary key
  - `@GeneratedValue` - Auto-generates ID values

---

### 3. **Controller Package** (`com.example.vocablearning.Controller`)

#### `HomeController.java`
- **Purpose**: Handles all HTTP requests and routes them to appropriate services
- **Responsibility**:
  - Maps URLs to handler methods
  - Receives user input from forms/AJAX calls
  - Calls service layer for business logic
  - Returns views (HTML pages) or JSON responses
  
- **Endpoints**:
  - `GET /` - Returns home page
  - `GET /add-word` - Returns add word form page
  - `GET /get-word-list` - Returns all words as JSON
  - `POST /add-word` - Saves new word to database
  - `POST /delete-word` - Deletes a word
  - `POST /generate-word-details` - Generates AI-powered word details

- **Why it exists**: Entry point for all client requests, separates routing logic from business logic

---

### 4. **Service Package** (`com.example.vocablearning.Service`)

#### `WordService.java` (Interface)
- **Purpose**: Defines contract for word-related operations
- **Responsibility**: Declares CRUD methods (Create, Read, Update, Delete)
- **Why it exists**: Provides abstraction and allows multiple implementations

#### `WordServiceImplement.java`
- **Purpose**: Implements business logic for word management
- **Responsibility**:
  - Converts between `Word` (DTO) and `WordEntity` (Database entity)
  - Uses `BeanUtils.copyProperties()` for object mapping
  - Calls `WordRepository` for database operations
  - Implements CRUD operations
  
- **Data Flow**:
  ```
  Word (DTO) → WordEntity → Database (via Repository)
  Database → WordEntity → Word (DTO) → Controller → Client
  ```

- **Why it exists**: Encapsulates business logic and data transformation

#### `WordRepository.java` (Interface)
- **Purpose**: Database access layer
- **Responsibility**:
  - Extends `JpaRepository<WordEntity, Long>`
  - Provides built-in methods: save(), findAll(), deleteById(), etc.
  - No implementation needed - Spring Data JPA generates it automatically
  
- **Why it exists**: Abstracts database operations, no need to write SQL queries

#### `AIService.java`
- **Purpose**: Integrates with Ollama AI for content generation
- **Responsibility**:
  - Generates word meanings using AI
  - Generates example sentences using AI
  - Uses Spring AI's `ChatClient` to communicate with Ollama
  - Provides fallback values if AI fails
  
- **Why it exists**: Separates AI logic from word management logic

---

## 🔄 Application Flow

### **Scenario 1: User Adds a Word Manually**

1. **User clicks "Add New Word" button** on home page
   - Browser sends: `GET /add-word`
   
2. **HomeController.addData()** receives request
   - Creates empty `Word` object
   - Returns `add-word.html` template
   
3. **User fills form** (word, meaning, sentence) and clicks "Submit"
   - Browser sends: `POST /add-word` with form data
   
4. **HomeController.addWord()** receives form data
   - Thymeleaf binds form fields to `Word` object
   - Calls `wordService.createWord(word)`
   
5. **WordServiceImplement.createWord()** processes the word
   - Creates new `WordEntity`
   - Copies properties from `Word` to `WordEntity` using `BeanUtils`
   - Calls `wordRepository.save(wordEntity)`
   
6. **WordRepository** (Spring Data JPA)
   - Generates SQL: `INSERT INTO words_db (word, meaning, sentence) VALUES (?, ?, ?)`
   - Saves to MySQL database
   
7. **Response flows back**
   - Service returns success message
   - Controller returns updated word list as JSON
   - Browser displays updated list

---

### **Scenario 2: User Uses AI to Generate Word Details**

1. **User enters a word** (e.g., "Ephemeral") and clicks "🤖 AI Generate"
   - JavaScript function `generateWordDetails()` is triggered
   
2. **JavaScript sends AJAX request**
   - `POST /generate-word-details?word=Ephemeral`
   
3. **HomeController.generateWordDetails()** receives request
   - Extracts word parameter
   - Calls `aiService.generateWordDetails(word)`
   
4. **AIService.generateWordDetails()** processes request
   - Creates empty `Word` object
   - Calls `generateMeaning(word)`
     - Sends prompt to Ollama: "Define the word 'Ephemeral'..."
     - Ollama (llama3.2 model) generates definition
     - Returns: "Lasting for a very short time"
   
5. **AIService continues**
   - Calls `generateSentence(word, meaning)`
     - Sends prompt: "Create example sentence using 'Ephemeral'..."
     - Ollama generates: "The beauty of cherry blossoms is ephemeral."
   
6. **Response flows back**
   - AIService returns complete `Word` object with AI-generated content
   - Controller returns JSON response
   - JavaScript receives JSON and populates form fields
   - User can review and submit

---

### **Scenario 3: User Views All Stored Words**

1. **User clicks "Get Stored Data"** button
   - Browser sends: `GET /get-word-list`
   
2. **HomeController.getWordList()** receives request
   - Calls `wordService.readWords()`
   
3. **WordServiceImplement.readWords()** processes request
   - Calls `wordRepository.findAll()`
   - Spring Data JPA generates: `SELECT * FROM words_db`
   - Returns `List<WordEntity>`
   
4. **Service transforms data**
   - Loops through each `WordEntity`
   - Converts to `Word` DTO using `BeanUtils.copyProperties()`
   - Returns `List<Word>`
   
5. **Controller returns JSON**
   - Spring automatically converts `List<Word>` to JSON
   - Browser receives and displays word list

---

### **Scenario 4: User Deletes a Word**

1. **User clicks delete button** (if implemented in UI)
   - JavaScript sends: `POST /delete-word?word=Ephemeral`
   
2. **HomeController.deleteWord()** receives request
   - Calls `wordService.deleteWord(word)`
   
3. **WordServiceImplement.deleteWord()** processes request
   - Removes word from in-memory list (current implementation)
   - Returns `true`
   
4. **Response flows back**
   - Controller returns boolean result
   - UI updates to reflect deletion

---

## 🗄️ Database Schema

**Table Name**: `words_db`

| Column   | Type         | Constraints                    |
|----------|--------------|--------------------------------|
| id       | BIGINT       | PRIMARY KEY, AUTO_INCREMENT    |
| word     | VARCHAR(255) | NOT NULL                       |
| meaning  | TEXT         |                                |
| sentence | TEXT         |                                |

---

## ⚙️ Configuration

### Database Configuration (`application.properties`)
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/vocablearning
spring.datasource.username=root
spring.datasource.password=Samsung@11
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
```

### AI Configuration (`application.properties`)
```properties
spring.ai.ollama.base-url=http://localhost:11434
spring.ai.ollama.chat.model=llama3.2
spring.ai.ollama.chat.options.temperature=0.7
```

---

## 🛠️ Prerequisites

1. **Java 17** or higher
2. **Maven 3.6+**
3. **MySQL Server** running on port 3306
4. **Ollama** installed and running on port 11434
   - Install Ollama: https://ollama.ai
   - Pull llama3.2 model: `ollama pull llama3.2`

---

## 🚀 How to Run

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd vocablearning
   ```

2. **Create MySQL database**
   ```sql
   CREATE DATABASE vocablearning;
   ```

3. **Update database credentials** in `application.properties`

4. **Start Ollama** (in separate terminal)
   ```bash
   ollama serve
   ```

5. **Run the application**
   ```bash
   ./mvnw spring-boot:run
   ```

6. **Access the application**
   - Open browser: http://localhost:8080

---

## 📁 Project Structure

```
vocablearning/
├── src/
│   ├── main/
│   │   ├── java/com/example/vocablearning/
│   │   │   ├── Controller/
│   │   │   │   └── HomeController.java          # HTTP request handler
│   │   │   ├── Entity/
│   │   │   │   └── WordEntity.java              # Database entity
│   │   │   ├── Model/
│   │   │   │   └── Word.java                    # Data transfer object
│   │   │   ├── Service/
│   │   │   │   ├── AIService.java               # AI integration
│   │   │   │   ├── WordRepository.java          # Database access
│   │   │   │   ├── WordService.java             # Service interface
│   │   │   │   └── WordServiceImplement.java    # Service implementation
│   │   │   └── VocablearningApplication.java    # Main application
│   │   └── resources/
│   │       ├── templates/
│   │       │   ├── home.html                    # Landing page
│   │       │   └── add-word.html                # Add word form
│   │       └── application.properties           # Configuration
│   └── test/
├── pom.xml                                      # Maven dependencies
└── README.md
```

---

## 🎯 Key Design Patterns

1. **MVC Pattern**: Separates presentation, business logic, and data access
2. **Dependency Injection**: Spring manages object creation and dependencies
3. **Repository Pattern**: Abstracts database operations
4. **DTO Pattern**: Separates API models from database entities
5. **Service Layer Pattern**: Encapsulates business logic

---

## 🔮 Future Enhancements

- [ ] Implement update word functionality
- [ ] Add user authentication
- [ ] Create word quiz feature
- [ ] Add word categories/tags
- [ ] Implement search functionality
- [ ] Add pronunciation audio
- [ ] Export words to PDF/CSV

---

## 📝 License

This project is open source and available under the MIT License.

---

## 👨‍💻 Author

Created as a Spring Boot learning project demonstrating integration of:
- RESTful APIs
- Database persistence
- AI-powered content generation
- Modern web UI

# API Documentation Generator

AI-powered REST API documentation generator using Spring Boot 3.4.0 and Ollama LLM.

## Features

- **Agentic AI Architecture**: Multi-agent system for parallel documentation generation
- **Source Code Analysis**: Parses Maven projects and extracts API metadata
- **Business Logic Extraction**: LLM-powered analysis of business rules and validations
- **Technical Workflow Documentation**: Step-by-step execution flow documentation
- **Batch Processing**: Excel-based batch processing for 100+ APIs
- **High Performance**: Async execution, caching, and parallel processing
- **Production-Ready**: Correlation ID tracking, i18n, comprehensive error handling

## Tech Stack

- **Java 21**
- **Spring Boot 3.4.0**
- **Ollama (llama3.1:8b)**
- **JavaParser 3.25.9**
- **Apache POI 5.2.5**
- **Caffeine Cache 3.1.8**

## Prerequisites

- Java 21+
- Maven 3.9+
- Docker & Docker Compose (for Ollama)
- Ollama with llama3.1:8b model

## Quick Start

### 1. Clone Repository

```bash
git clone <repository-url>
cd api-documentation-generator
```

### 2. Start Ollama with Docker

```bash
docker-compose up -d ollama
docker-compose up ollama-setup  # Download model
```

### 3. Build Application

```bash
mvn clean package
```

### 4. Run Application

```bash
mvn spring-boot:run
```

### Or run with Docker:

```bash
docker-compose up -d
```

### 5. Verify Health

```bash
curl http://localhost:8080/health
curl http://localhost:8080/health/ollama
```

### Usage-Single API Documentation


curl -X POST http://localhost:8080/api/v1/documentation/generate \
  -H "Content-Type: application/json" \
  -d '{
    "projectPath": "/path/to/your/maven/project",
    "apiUri": "/api/v1/users",
    "httpMethod": "GET",
    "includeBusinessLogic": true,
    "includeTechnicalWorkflow": true,
    "preferredLanguage": "en"
  }'
  
  
### Batch Processing from Excel

Prepare Excel file with columns:


- API_URI
- HTTP_METHOD
- BUSINESS_LOGIC (empty, will be filled)
- TECHNICAL_WORKFLOW (empty, will be filled)



curl -X POST http://localhost:8080/api/v1/documentation/batch/excel \
  -H "Content-Type: application/json" \
  -d '{
    "projectPath": "/path/to/maven/project",
    "excelFilePath": "/path/to/apis.xlsx",
    "apiUriColumnName": "API_URI",
    "httpMethodColumnName": "HTTP_METHOD",
    "businessLogicColumnName": "BUSINESS_LOGIC",
    "technicalWorkflowColumnName": "TECHNICAL_WORKFLOW",
    "startRow": 1,
    "preferredLanguage": "en"
  }'
 

### Architecture
Agentic AI Workflow

```bash
1. CodeReaderAgent
   ↓ (parses source code)
2. BusinessLogicAgent ←→ TechnicalWorkflowAgent
   ↓ (parallel LLM analysis)
3. DocumentationFormatterAgent
   ↓
4. DocumentationResponse
```

### Environment Variables
bash
export SPRING_PROFILES_ACTIVE=prod
export OLLAMA_BASE_URL=http://ollama-service:11434
export OLLAMA_MODEL=llama3.1:8b

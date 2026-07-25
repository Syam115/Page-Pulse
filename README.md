# Page-Pulse

A Spring Boot backend service that audits web pages by analyzing HTML content and returning comprehensive metrics about page structure, accessibility, and performance.

## Setup

### Prerequisites
- Java 21 or higher
- Maven 3.6+ (or use the included `mvnw` wrapper)
- Internet access for fetching web pages

### Installation

1. **Clone and navigate to the project**:
```bash
cd Page-Pulse
cd backend
```

2. **Build the project**:

```bash
./mvnw clean install
```
On Windows (from backend directory):
```cmd
mvnw.cmd clean install
```

3. **Run the application**:
```bash
./mvnw spring-boot:run
```
Or Windows (from backend directory):
```cmd
mvnw.cmd spring-boot:run
```

The server will start at `http://localhost:8080`

4. **Run tests**:
```bash
./mvnw test
```

### Dependencies
- **Spring Boot 4.1.0**: Web framework and dependency injection
- **JSoup**: HTML parsing and fetching
- **Spring AI JSoup**: Document reading for web scraping
- **JUnit 5**: Testing framework
- **Mockito 5.2.0**: Mocking for unit tests

## API Contract

### Endpoint: `/api/audit`

**Method**: `POST`

**Description**: Fetches and analyzes a web page, returning comprehensive audit metrics.

#### Request

```json
{
  "url": "https://example.com"
}
```

**Content-Type**: `application/json`

**Fields**:
- `url` (string, required): The URL to audit. Must start with `http://` or `https://`

#### Response - Success (200 OK)

```json
{
  "httpStatus": 200,
  "responseTime": 245,
  "pageTitle": "Example Domain",
  "metaDescription": "Example of an internet domain for use in examples and documentation",
  "h1Count": 1,
  "missingAltCount": 2,
  "wordCount": 340
}
```

**Fields**:
- `httpStatus` (integer): HTTP status code of the fetched page (e.g., 200, 404, 500)
- `responseTime` (long): Time taken to fetch the page in milliseconds
- `pageTitle` (string): Content of the `<title>` tag
- `metaDescription` (string): Content of the meta description tag or "No meta description found"
- `h1Count` (integer): Number of `<h1>` heading tags on the page
- `missingAltCount` (integer): Number of images missing the `alt` attribute or with empty `alt=""`
- `wordCount` (integer): Approximate word count of the page body text (split by whitespace)

#### Response - Error Cases

**400 Bad Request** - Invalid URL:
```json
{
  "timestamp": "2026-07-24T18:32:00+05:30",
  "statusCode": 400,
  "error": "INVALID_URL",
  "message": "URL cannot be null or empty",
  "path": "/api/audit"
}
```

**408 Request Timeout** - Connection timeout (5 seconds):
```json
{
  "timestamp": "2026-07-24T18:32:00+05:30",
  "statusCode": 408,
  "error": "TIMEOUT",
  "message": "Targeted website took too long to respond",
  "path": "/api/audit"
}
```

**415 Unsupported Media Type** - Non-HTML content:
```json
{
  "timestamp": "2026-07-24T18:32:00+05:30",
  "statusCode": 415,
  "error": "NOT_AN_HTML_PAGE",
  "message": "Content-Type is not HTML",
  "path": "/api/audit"
}
```

**500 Internal Server Error** - Unexpected errors:
```json
{
  "timestamp": "2026-07-24T18:32:00+05:30",
  "statusCode": 500,
  "error": "SERVER_ERROR",
  "message": "Internal Server Error",
  "path": "/api/audit"
}
```

### Example cURL Request

```bash
curl -X POST http://localhost:8080/api/audit \
  -H "Content-Type: application/json" \
  -d '{"url": "https://www.github.com"}'
```

### Example Python Request

```python
import requests

response = requests.post(
    'http://localhost:8080/api/audit',
    json={'url': 'https://www.github.com'}
)
print(response.json())
```

## Design Decisions

### 1. **Using Milliseconds Instead of Minutes for Response Time**

**Decision**: Report response time in milliseconds (not seconds or minutes).

**Reasoning**:
- **Precision**: Most HTTP responses complete in 100-500ms. Using minutes would always return 0 for sub-60-second requests, making the metric useless.
- **Industry Standard**: Web performance tools (Google PageSpeed, WebPageTest, Lighthouse) all report in milliseconds, providing consistency with industry practices.
- **Client Flexibility**: Clients can easily convert milliseconds to any unit they need (divide by 1000 for seconds, 60000 for minutes), but can't recover precision if we provide only minutes.
- **Implementation Impact**: This decision caught a critical bug in the original code that was using `ChronoUnit.MINUTES` instead of `ChronoUnit.MILLIS`, which would have silently returned incorrect data.

### 2. **Separate Validation Layer with Specific Exception Types**

**Decision**: Validate input in the service layer using custom exception types (InvalidUrlFormatException, InvalidContentTypeException) before attempting network operations.

**Reasoning**:
- **Fail Fast**: Catching invalid URLs before making network requests saves bandwidth and server resources.
- **Specific Error Handling**: Different exceptions map to different HTTP status codes (400 for client errors, 408 for timeouts, 415 for content type). This allows clients to implement retry logic and recover appropriately.
- **Separation of Concerns**: The GlobalExceptionHandler translates domain exceptions into HTTP responses, keeping business logic separate from HTTP concerns.
- **Testability**: Custom exceptions are easier to mock and test than catching IOException or URISyntaxException.
- **Example**: If we validated only after fetching, a request to "ftp://example.com" would waste network bandwidth before we reject it. With our approach, it fails in ~1ms during URI parsing.

### 3. **Including HTTP Status Code in the Audit Response**

**Decision**: Include the original page's HTTP status code in the audit response (not just success/failure).

**Reasoning**:
- **Complete Picture**: A page returning 404 is structurally "valid" HTML (we can parse it), but semantically different from a 200 response. Clients need this context.
- **Business Logic**: SEO tools, monitoring systems, and health checks need to distinguish between:
  - 200 OK pages (healthy)
  - 301/302 redirects (pages have moved)
  - 404 Not Found (pages are missing)
  - 500 Internal Server Error (backend issues)
- **No Data Loss**: Including the status code adds one integer field and costs nothing, but enables powerful use cases.
- **Example**: A website audit tool can track that a competitor's page returns 200 with good SEO (high word count, proper meta tags) vs. a competitor's page returns 410 Gone (page removed from index).
- **Trade-off**: The endpoint could fail entirely on non-200 responses (return error), but that would lose valuable diagnostic information about why the page isn't serving properly.

## Project Structure

```
Page-Pulse/
├── src/
│   ├── main/java/com/digitalheroes/Page/Pulse/
│   │   ├── PagePulseApplication.java          # Spring Boot entry point
│   │   ├── controller/
│   │   │   └── AuditController.java           # HTTP endpoint handler
│   │   ├── service/
│   │   │   └── AuditService.java              # Core audit logic
│   │   ├── dto/
│   │   │   ├── AuditRequest.java              # Request DTO
│   │   │   └── AuditResponse.java             # Response DTO
│   │   └── exception/
│   │       ├── GlobalExceptionHandler.java    # Exception → HTTP response mapping
│   │       ├── InvalidUrlFormatException.java # Custom exception for invalid URLs
│   │       ├── InvalidContentTypeException.java # Custom exception for non-HTML
│   │       └── ErrorResponse.java             # Error response DTO
│   ├── test/java/com/digitalheroes/Page/Pulse/
│   │   ├── service/AuditServiceTest.java      # 8 unit tests for service logic
│   │   └── controller/AuditControllerTest.java # 2 unit tests for controller
│   └── resources/
│       └── application.properties             # Spring Boot configuration
├── backend/
│   ├── pom.xml                                 # Maven dependencies
│   ├── mvnw & mvnw.cmd                         # Maven wrapper scripts
└── .gitignore                                 # Git ignore rules
```

## Testing

The project includes 11 comprehensive tests covering:

### Service Tests (8 tests)
- ✅ **Happy path**: Successfully audit a valid HTML page
- ✅ **Invalid URL - null**: Reject null URL
- ✅ **Invalid URL - empty**: Reject empty URL
- ✅ **Invalid URL - wrong protocol**: Reject FTP and other non-HTTP protocols
- ✅ **Non-HTML content**: Reject JSON, images, and other non-HTML responses
- ✅ **Timeout**: Handle 5-second connection timeout
- ✅ **No meta description**: Handle pages without meta description tag
- ✅ **Empty body**: Handle pages with no body text (word count = 0)

### Controller Tests (2 tests)
- ✅ **Successful request**: Controller returns service response
- ✅ **Invalid URL error**: Controller propagates service exceptions

### Integration Tests (1 test)
- ✅ **Application context**: Spring Boot context loads correctly

Run tests with:
```bash
./mvnw test
```

## Error Handling Strategy

The application uses a layered error handling approach:

1. **Service Layer**: Validates input and throws domain-specific exceptions
2. **Global Exception Handler**: Catches exceptions and maps them to HTTP status codes
3. **Client Response**: Receives structured error JSON with timestamp, status code, error type, and message

This ensures:
- No unhandled exceptions crash the server
- Clients receive predictable error responses
- Debugging is easier with error codes and messages

## Performance Considerations

- **Connection Timeout**: 5 seconds maximum wait per request (prevents hanging on slow/dead servers)
- **No Caching**: Each request fetches fresh data (can be added later)
- **Synchronous Operation**: Request completes when page is fetched (not async)
- **Memory**: Parsed DOM held in memory only for response generation, then garbage collected

## Future Enhancements

- Add response caching with TTL
- Support for async requests (return job ID, poll for results)
- Additional metrics: load time breakdown, JavaScript errors, CSS validation
- Screenshot generation
- SEO scoring algorithm
- Batch audit API
- Rate limiting and API keys
- Webhook notifications for audit completion

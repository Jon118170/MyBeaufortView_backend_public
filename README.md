# MyBeaufortView Backend

> This repository is a sanitized public version of MyBeaufortView. Active development history is maintained in a private repository.

MyBeaufortView is a Spring Boot REST API powering a community-focused photography platform for sharing and discovering images captured in Beaufort, South Carolina.

The backend provides authentication, photography feeds, location-based discovery, search, post interactions, collections, user profiles, image uploads and media processing, real-time notifications, and photographer commission requests for a React/Vite frontend.

## Project Highlights

- Java 21 with Spring Boot 3.5
- Stateless JWT authentication using Spring Security
- Role-based and resource-level authorization
- Layered Controller → Service → Repository architecture
- DTO-based response models for public-facing resources
- PostgreSQL persistence with Spring Data JPA
- Flyway-managed database migrations
- Paginated photography feeds and collections
- Location-based photography discovery
- Search, image tags, and similar-post discovery
- Idempotent like/unlike interactions
- AWS S3 presigned upload workflow
- Thumbnail and background media-processing support
- OpenAI-backed image-tagging integration
- Server-Sent Events (SSE) for real-time notifications
- Photographer commission request workflow
- Spring Boot Actuator and Prometheus metrics
- Unit and integration testing with JUnit, MockMvc, and Testcontainers
- Separate Maven Surefire and Failsafe test phases

## Project Vision

MyBeaufortView is a community-driven photography platform designed to showcase the natural beauty of Beaufort, South Carolina through local photography.

Unlike general-purpose social platforms that mix photography with unrelated content, MyBeaufortView focuses on outdoor and local photography captured around Beaufort.

The platform allows photographers and visitors to share images, discover photography by location, explore other photographers, organize posts into collections, interact with photography, and connect with photographers through commission requests.

## User Roles

### Guest

Guests can browse publicly available content without creating an account.

Public functionality includes:

- Browsing photography posts
- Viewing individual posts
- Exploring Beaufort locations
- Viewing location-based feeds
- Searching photography
- Viewing public user profiles
- Viewing public user posts
- Browsing public collections
- Viewing post like counts

### Privileged User

Registered users with the `PRIVILEGED_USER` role can interact with the platform.

Authenticated functionality includes:

- Creating and managing posts
- Liking and unliking posts
- Managing a profile
- Creating and managing collections
- Uploading images
- Receiving notifications
- Sending and responding to commission requests

### Admin

Users with the `ADMIN` role have elevated permissions for platform administration and moderation.

Administrative functionality includes protected user-management operations and access to restricted application-monitoring endpoints.

## Core Platform Features

### Photography Posts

Users can create, browse, edit, and delete photography posts.

Post responses include information such as:

- Description
- Original image URL
- Thumbnail URL
- Creation timestamp
- Author information
- Beaufort location metadata
- Like count
- Current-user like state
- Image tags

The primary feed uses pagination and defaults to newest-first ordering.

### Location-Based Discovery

Photography can be browsed by Beaufort-area location.

Location responses include:

- Location name
- URL-friendly slug
- Latitude and longitude
- Description
- Number of associated posts

Each location also exposes a paginated photography feed.

### Search and Similar Posts

The backend provides a public search API for discovering posts.

Posts can also expose automatically generated image tags, and the API supports retrieving similar posts based on the application's discovery logic.

### Likes

Authenticated users can like and unlike posts.

The API also provides:

- Public like counts
- Personalized liked/not-liked state
- Idempotent interaction behavior

### Collections

Users can organize photography into collections.

Collections support:

- Public collection browsing
- Paginated collection listings
- User-specific collection listings
- Collection visibility
- Cover images
- Post counts
- Adding posts to collections
- Removing posts from collections
- Ownership-aware update and delete operations

### User Profiles

Public user profiles expose photography-oriented information including:

- Username
- Display name
- Biography
- Profile image
- Account creation date
- Post count
- Collection count
- Share path

Authenticated users can update their own profile.

### Image Uploads

When AWS S3 integration is enabled, authenticated clients can request a presigned upload URL from the backend.

The frontend can then upload image data directly to S3 without routing the image payload through the application server.

### Media Processing

The application tracks background media-processing jobs associated with posts.

Media status responses expose:

- Job ID
- Job type
- Processing status
- Attempt count
- Error information when applicable

The backend also includes thumbnail-processing and image-tagging infrastructure.

### Real-Time Notifications

Authenticated users have access to an application notification system supporting:

- Notification history
- Unread counts
- Mark-as-read operations
- Notification deletion
- Real-time delivery using Server-Sent Events

### Commission Requests

Authenticated users can submit photography commission requests.

The workflow supports:

- Creating requests
- Viewing sent requests
- Viewing received requests
- Accepting requests
- Declining requests
- Associating requests with a post

Commission responses track requester and photographer information, status, message content, related post, and timestamps.

## Technology Stack

### Backend

- Java 21
- Spring Boot 3.5.4
- Spring MVC
- Spring Data JPA
- Spring Security
- Jakarta Bean Validation
- JWT authentication using JJWT
- PostgreSQL
- Flyway

### Storage and Media

- AWS SDK for Java
- Amazon S3 presigned uploads
- Thumbnailator
- Background media jobs
- OpenAI image-tagging integration

### Observability

- Spring Boot Actuator
- Micrometer
- Prometheus registry

### Testing

- JUnit
- Spring Boot Test
- MockMvc
- Spring Security Test
- H2 for selected tests
- Testcontainers
- PostgreSQL Testcontainers
- Maven Surefire
- Maven Failsafe

## Architecture

The backend follows a layered application architecture:

```text
HTTP Request
     │
     ▼
Controller
     │
     ▼
Service
     │
     ▼
Repository
     │
     ▼
PostgreSQL
```

**Controllers** define REST endpoints, handle request validation, and produce API responses.

**Services** contain application and business logic.

**Repositories** provide database access through Spring Data JPA.

**DTOs** provide stable public-facing response models for posts, profiles, collections, locations, notifications, commissions, media state, and other API resources.

Cross-cutting infrastructure handles authentication, authorization, rate limiting, storage, media processing, real-time notifications, exception handling, and observability.

## API Overview

The following is a high-level overview of the current API.

### Authentication

```text
POST /api/auth/register
POST /api/auth/login
```

Registration and login are public.

A successful login returns a JWT together with basic account information:

```json
{
  "token": "<jwt>",
  "email": "user@example.com",
  "role": "PRIVILEGED_USER",
  "username": "photographer"
}
```

Protected requests use:

```text
Authorization: Bearer <token>
```

### Posts

```text
GET    /api/posts
GET    /api/posts/{id}
GET    /api/posts/all
GET    /api/posts/{id}/similar

POST   /api/posts
PUT    /api/posts/{id}
DELETE /api/posts/{id}
```

Post reads are public.

Creating, updating, or deleting posts requires an authenticated `PRIVILEGED_USER` or `ADMIN`, with additional ownership/authorization rules enforced by the application.

### Post Interactions

```text
POST   /api/posts/{postId}/like
DELETE /api/posts/{postId}/like
GET    /api/posts/{postId}/liked
GET    /api/posts/{postId}/likes/count
```

Like and unlike operations require authentication.

Personalized liked state also requires authentication.

Like counts are public.

### Search

```text
GET /api/search?q={query}&sort={sort}
```

Search is public.

### Locations

```text
GET /api/locations
GET /api/locations/{slug}
GET /api/locations/{slug}/posts
```

Location endpoints are public.

### Collections

```text
GET    /api/collections
GET    /api/collections/{collectionId}

POST   /api/collections
PUT    /api/collections/{collectionId}
DELETE /api/collections/{collectionId}

POST   /api/collections/{collectionId}/posts/{postId}
DELETE /api/collections/{collectionId}/posts/{postId}
```

Collection reads are public.

Collection creation and modification require authentication and are subject to ownership rules.

### User Collections

```text
GET /api/users/{userId}/collections
```

### Public Profiles

```text
GET /api/users/{userId}/profile
GET /api/users/{userId}/posts
```

### Current User Profile

```text
PUT /api/users/me/profile
```

### User Management

```text
POST   /api/user
GET    /api/users
GET    /api/user/{id}
PUT    /api/user/{id}
DELETE /api/user/{id}
```

User-management endpoints are protected through role-based and self-access authorization rules.

### Uploads

```text
POST /api/uploads/presign
```

This endpoint requires authentication and is registered only when:

```text
AWS_S3_ENABLED=true
```

### Media Processing

```text
GET /api/media/{postId}/status
```

Media-processing status is publicly readable.

### Notifications

```text
GET    /api/notifications
GET    /api/notifications/unread-count
GET    /api/notifications/stream
PATCH  /api/notifications/{id}/read
DELETE /api/notifications/{id}
```

Notification endpoints require authentication.

`GET /api/notifications/stream` uses Server-Sent Events for real-time delivery.

### Commission Requests

```text
POST  /api/commissions
GET   /api/commissions/received
GET   /api/commissions/sent
PATCH /api/commissions/{id}/accept
PATCH /api/commissions/{id}/decline
```

Commission operations require authentication.

## Example Post Feed Response

`GET /api/posts` returns a paginated response using the application's shared `PageResponse` format.

```json
{
  "items": [
    {
      "id": 3,
      "description": "Golden hour over Beaufort!",
      "imageUrl": "https://example.com/photos/image.jpg",
      "thumbnailUrl": "https://example.com/photos/image-thumbnail.jpg",
      "createdAt": "2026-02-27T02:34:54Z",
      "author": {
        "id": 1,
        "username": "photographer",
        "name": "Example Photographer",
        "avatarUrl": "https://example.com/avatar.jpg"
      },
      "location": {
        "id": 1,
        "name": "Hunting Island",
        "slug": "hunting-island",
        "latitude": 32.3738,
        "longitude": -80.4512,
        "description": "A Beaufort-area photography location.",
        "postCount": 14
      },
      "likeCount": 8,
      "likedByMe": false,
      "tags": [
        "sunset",
        "beach",
        "landscape"
      ]
    }
  ],
  "page": 0,
  "size": 20,
  "totalItems": 1,
  "totalPages": 1
}
```

## Security

The backend uses Spring Security with stateless JWT authentication.

HTTP Basic authentication and form login are disabled, and the application does not use server-side authentication sessions.

Spring method security is enabled for fine-grained authorization.

### Roles

The application uses:

- `PRIVILEGED_USER`
- `ADMIN`

### Public Access

Public functionality includes:

- Authentication registration and login
- Post reads
- Post like counts
- Public user profiles
- Public user posts
- Public collection reads
- User collection listings
- Location browsing
- Location feeds
- Search
- Media-processing status
- Actuator health
- Actuator application information

### Authenticated Access

Authentication is required for functionality including:

- Like and unlike operations
- Personalized like state
- Profile updates
- Collection modifications
- S3 presigned upload requests
- Notifications
- Commission requests

Post creation, update, and deletion additionally require the appropriate application role.

### Actuator Security

The application exposes:

```text
health
info
metrics
prometheus
```

Access rules are:

- `/actuator/health/**` — public
- `/actuator/info` — public
- `/actuator/prometheus` — `ADMIN`
- `/actuator/metrics/**` — `ADMIN`
- Any other actuator route — `ADMIN`

Health details are configured not to expose internal information publicly.

## Database

The application uses PostgreSQL with Flyway for schema versioning.

Hibernate schema management is configured as:

```text
validate
```

This means the application validates the database schema rather than creating or mutating it automatically.

Production Flyway migrations are located in:

```text
src/main/resources/db/migration
```

The schema includes support for:

- Users
- User profiles
- Posts
- Locations
- Post tags
- Collections
- Collection entries
- Likes
- Media jobs
- Notifications
- Commission requests

Development-only migration data is maintained separately from the main production migration path.

## Environment Configuration

Runtime secrets and deployment-specific configuration are supplied through environment variables rather than committed application credentials.

Use `.env.example` as a reference for local configuration. The real `.env` file should remain outside version control.

### Core Configuration

```text
DATABASE_URL
DATABASE_USERNAME
DATABASE_PASSWORD
JWT_SECRET
OPENAI_API_KEY
```

`JWT_SECRET` must be a Base64-encoded secret representing at least 256 bits.

### AWS S3 Configuration

S3 integration is disabled by default.

When S3 is enabled, configure:

```text
AWS_S3_BUCKET
AWS_S3_REGION
AWS_S3_ENABLED
```

Current defaults include:

```text
AWS_S3_REGION=us-east-1
AWS_S3_ENABLED=false
```

### OpenAI Configuration

Image-tagging integration uses:

```text
OPENAI_API_KEY
OPENAI_MODEL
OPENAI_BASE_URL
```

Current defaults for the optional model and base URL settings are:

```text
OPENAI_MODEL=gpt-4.1-mini
OPENAI_BASE_URL=https://api.openai.com/v1
```

Do not commit real database passwords, JWT secrets, AWS credentials, API keys, or the local `.env` file.

## Running Locally

### Requirements

- Java 21
- Docker
- Maven, or the included Maven Wrapper

### Configure the Environment

Create a local `.env` using `.env.example` as a reference and replace placeholder values with your local configuration.

The `.env` file is intentionally excluded from version control.

### PostgreSQL with Docker Compose

The provided Compose configuration includes PostgreSQL 16.

To start PostgreSQL:

```bash
docker compose up -d postgres
```

The development database is exposed on:

```text
localhost:5432
```

### Run the Backend Locally

Provide the required environment variables, then run:

macOS/Linux:

```bash
./mvnw spring-boot:run
```

Windows PowerShell:

```powershell
.\mvnw.cmd spring-boot:run
```

The backend uses port `8080` by default.

## Docker

The repository includes:

- `Dockerfile`
- `docker-compose.yml`
- `.dockerignore`

The Compose configuration defines PostgreSQL and backend services.

S3 is disabled in the default Compose configuration.

Deployment secrets should be supplied through environment variables and must not be committed to the repository.

## Testing

The Maven build separates standard tests from integration tests.

### Unit / Standard Test Phase

Maven Surefire runs tests matching:

```text
**/*Test.java
```

Run them with:

macOS/Linux:

```bash
./mvnw test
```

Windows PowerShell:

```powershell
.\mvnw.cmd test
```

### Integration Test Phase

Maven Failsafe runs tests matching:

```text
**/*IT.java
```

The integration suite includes security and PostgreSQL-backed testing.

Testcontainers is used to provide isolated PostgreSQL instances for integration and Flyway verification.

### Complete Verification

Run both test phases with:

macOS/Linux:

```bash
./mvnw clean verify
```

Windows PowerShell:

```powershell
.\mvnw.cmd clean verify
```

Docker must be running for tests that use Testcontainers.

## OpenAPI

The project includes Springdoc OpenAPI support as a dependency.

OpenAPI API-doc generation and Swagger UI are currently **disabled by default** in `application.properties`:

```properties
springdoc.api-docs.enabled=false
springdoc.swagger-ui.enabled=false
```

They can be enabled in an appropriate development configuration when interactive API documentation is needed.

## Frontend Repository

The frontend is implemented separately using React and Vite:

[MyBeaufortView Frontend](https://github.com/Jon118170/MyBeaufortView_frontend)

## Repository Note

This repository contains a sanitized public snapshot of the MyBeaufortView backend.

Active development history is maintained separately in a private repository.

# MyBeaufortView Backend

MyBeaufortView is a Spring Boot REST API powering a community-focused photography platform for sharing and discovering images captured in Beaufort, South Carolina.

The backend provides secure authentication, post feeds, post interactions, collections, profile management, and image upload capabilities for a React/Vite frontend application.

## Project Highlights

- Secure JWT authentication with stateless session management
- Layered Spring Boot architecture (Controller → Service → Repository)
- DTO-based API responses to avoid exposing internal entities
- Idempotent like/unlike interaction design
- AWS S3 presigned upload pipeline for scalable image storage
- Flyway-managed database migrations
- Integration testing using Testcontainers
- Role-based authorization using Spring Security
- Observability support with Spring Boot Actuator

## Project Vision

MyBeaufortView is a community-driven photography platform designed to showcase the natural beauty of Beaufort, South Carolina through local photography.

Unlike traditional social media platforms that mix photography with unrelated content, MyBeaufortView focuses exclusively on outdoor and local photography captured in Beaufort.

The platform allows photographers and visitors to share their images, explore posts from others, and organize photos into collections while maintaining a clean and focused browsing experience.

## User Roles

The platform supports three types of users.

### Guest User
Guests can browse the platform and view posts without creating an account. They can explore photos captured in Beaufort but cannot interact with posts until they register.

### Privileged User
Registered users can create posts, like other posts, manage their own content, and organize photos into collections.

### Admin User
Administrators manage the platform by moderating posts and users. They can delete inappropriate content, deactivate accounts, or enforce platform policies.

## Core Platform Features

- Public feed displaying photography posts from Beaufort
- Post detail pages showing image, author, and metadata
- Secure authentication for registered users
- Ability for users to upload and manage their own posts
- Idempotent like/unlike interactions
- Collections that allow users to organize posts
- Role-based access control for administrators
- Post moderation and user management tools

## Product Epics

### Basic Posting and Sharing
Users can upload, view, edit, and delete photography posts.

### Viewing and Discovering Content
Users can browse posts, view individual images, and explore other users' profiles.

### Navigation and User Experience
The application provides intuitive navigation so users can easily discover content and manage their posts.

### Authentication and Role-Based Access Control
The platform supports secure login and role-based permissions for guest users, registered users, and administrators.

### Platform Visibility
A landing page introduces the platform and explains its purpose to new visitors.

## Tech Stack

### Backend
- Java 21
- Spring Boot
- Spring Security
- JWT Authentication
- PostgreSQL
- Flyway database migrations

### Storage
- AWS S3 presigned uploads

### Testing
- JUnit
- Spring Boot Test
- MockMvc
- Testcontainers

## Architecture

The backend follows a layered architecture.

Controllers handle HTTP requests and responses.

Services contain the business logic of the application.

Repositories interact with the PostgreSQL database using Spring Data JPA.

DTOs are used to structure API responses and prevent exposing internal entities.

Image uploads use a presigned URL flow where the backend generates a temporary upload URL and the frontend uploads directly to AWS S3.

## API Overview

### Authentication

POST /api/auth/login

### Posts

GET /api/posts
GET /api/posts/{id}

### Post Interactions

POST /api/posts/{postId}/like
DELETE /api/posts/{postId}/like
GET /api/posts/{postId}/liked
GET /api/posts/{postId}/likes/count

### Users

GET /api/users
GET /api/users/{id}

### User Profiles

GET /api/users/{id}/profile
PUT /api/users/me/profile

### Collections

GET /api/collections
GET /api/collections/{id}
POST /api/collections
PUT /api/collections/{id}
DELETE /api/collections/{id}

### User Collections

GET /api/users/{userId}/collections

### Uploads

POST /api/uploads/presign

## Security

The backend uses Spring Security with JWT-based authentication.

Authentication is stateless and each request must include a valid JWT token in the Authorization header.

Example:

Authorization: Bearer <token>

### Roles

The system supports two primary roles:

- ADMIN
- PRIVILEGED_USER

Access to endpoints is restricted based on role permissions.

### Public Endpoints

These endpoints do not require authentication:

POST /api/auth/**
GET /api/posts
GET /api/posts/{id}
GET /api/users/{id}/profile
GET /api/users/{id}/posts

### Authenticated Endpoints

Require a valid JWT token:

POST /api/posts/{postId}/like
DELETE /api/posts/{postId}/like
PUT /api/users/me/profile
POST /api/collections
PUT /api/collections/{id}
DELETE /api/collections/{id}

### Admin Endpoints

Certain administrative operations require the ADMIN role, including:

GET /api/users
POST /api/users
DELETE /api/users/{id}

### Actuator Security

Application monitoring endpoints are secured:

- `/actuator/health` and `/actuator/info` are public
- `/actuator/prometheus` and `/actuator/metrics` require ADMIN access
- All other actuator endpoints require ADMIN privileges

The API is fully stateless and does not use server-side sessions. All authentication is performed using JWT tokens.

Spring Security method-level authorization is enabled to allow fine-grained access control within service or controller methods.

## Database

The project uses PostgreSQL with Flyway migrations to manage schema changes.

Migration scripts are located in:

src/main/resources/db/migration

Example migrations include:

- users table
- posts table
- collections and collection entries
- likes system
- user profile fields

## Running the Project Locally

### Example Post Response

```
{
  "content": [
    {
      "id": 3,
      "description": "Golden hour over Beaufort!",
      "imageUrl": "https://s3.amazonaws.com/.../image.jpg",
      "createdAt": "2026-02-27T02:34:54Z",
      "author": {
        "id": 1,
        "username": "admin",
        "name": "Dev Admin"
      },
      "likeCount": 0,
      "likedByMe": false
    }
  ],
  "totalPages": 1,
  "totalElements": 3,
  "size": 10,
  "number": 0,
  "last": true
}
```
### Requirements
- Java 21
- Docker
- Maven

### Start database

docker compose up -d

### Run backend

./mvnw spring-boot:run

### Run tests

./mvnw clean verify

## Environment Variables

The application requires several environment variables:

- DATABASE_URL
- DATABASE_USERNAME
- DATABASE_PASSWORD
- JWT_SECRET
- AWS_S3_BUCKET
- AWS_S3_REGION
- AWS_S3_ENABLED

## Testing

The project includes several levels of automated tests:

- Controller tests for REST endpoints
- Repository tests for database interactions
- Service layer tests
- Security integration tests
- Flyway migration smoke tests
- Testcontainers integration tests

Run tests with:

./mvnw clean verify

## Frontend Repository

The frontend application for this project is implemented using React and Vite in a separate repository.

https://github.com/Jon118170/MyBeaufortView_frontend

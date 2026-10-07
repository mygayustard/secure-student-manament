# Secure Student Management API

## Authentication Setup

This section documents the authentication and authorization API only. Student CRUD endpoints are not implemented yet.

Requirements: Java 17 or newer and PostgreSQL. Create a PostgreSQL database named `studentmanagement` with the local credentials in `src/main/resources/application.properties`, then start the app. Flyway applies `src/main/resources/db/migration/V1__init.sql`; Hibernate uses `ddl-auto=validate` and does not create or alter tables.

```sh
./mvnw spring-boot:run
```

All values below are fixed development settings in `application.properties`. Change the database credentials to match your local PostgreSQL instance. Replace the JWT key and seeded account passwords before using the application outside local development.

### Fixed Development Values

| Property | Value | Purpose |
| --- | --- | --- |
| `spring.datasource.url` | `jdbc:postgresql://localhost:5432/studentmanagement` | PostgreSQL JDBC URL |
| `spring.datasource.username` | `postgres` | Local database user |
| `spring.datasource.password` | `postgres` | Local database password |
| `jwt.secret` | `dev-only-change-me-secret-with-32-bytes-minimum-please` | Development-only HS256 key; at least 256 bits |
| `jwt.access-expiration-ms` | `900000` | Access-token lifetime (15 minutes) |
| `jwt.refresh-expiration-ms` | `604800000` | Refresh-token lifetime (7 days) |
| `app.cors.allowed-origin` | `http://localhost:3000` | The single allowed browser origin |
| `app.seed.admin.*` | `admin`, `admin@example.com`, `Admin12345` | Development ADMIN credentials |
| `app.seed.teacher.*` | `teacher`, `teacher@example.com`, `Teacher12345` | Development TEACHER credentials |

The seed credentials are for local development only. Seeding is idempotent by role; startup fails if a seed identity conflicts with another account.

## Endpoints

| Method | Path | Access | Purpose |
| --- | --- | --- | --- |
| `POST` | `/api/auth/register` | Public | Register a STUDENT account; returns 201 and a password-free user DTO |
| `POST` | `/api/auth/login` | Public | Verify credentials and issue access and refresh tokens |
| `POST` | `/api/auth/refresh` | Public | Rotate a refresh token and issue a new access token |
| `POST` | `/api/auth/logout` | Public | Revoke the supplied refresh token; idempotent |
| `GET` | `/api/users` | ADMIN | List users as password-free DTOs |
| `GET` | `/api/users/me` | Any authenticated user | Return the current user's password-free DTO |

Registration requires a 3–50 character username, valid email, and a password of at least 8 characters containing a letter and a digit. Self-registration always assigns `STUDENT`; client-supplied role values are not accepted.

## Roles and Permissions

| Role | Permissions |
| --- | --- |
| `ADMIN` | `GET /api/users`; all student operations |
| `TEACHER` | Authenticated profile; student GET/POST/PUT |
| `STUDENT` | Authenticated profile; student GET |

All other non-auth routes require authentication.

## Student CRUD

All student endpoints require a valid access token. Successful responses contain DTOs and never expose persistence entities.

| Method | Path | Access | Success |
| --- | --- | --- | --- |
| `POST` | `/api/students` | ADMIN, TEACHER | 201 Created |
| `GET` | `/api/students` | ADMIN, TEACHER, STUDENT | 200 OK |
| `GET` | `/api/students/{id}` | ADMIN, TEACHER, STUDENT | 200 OK |
| `PUT` | `/api/students/{id}` | ADMIN, TEACHER | 200 OK |
| `DELETE` | `/api/students/{id}` | ADMIN | 204 No Content |

| Operation | ADMIN | TEACHER | STUDENT |
| --- | --- | --- | --- |
| View (list and by id) | Yes | Yes | Yes |
| Create | Yes | Yes | No |
| Update | Yes | Yes | No |
| Delete | Yes | No | No |

Create and update accept the same complete request body. Phone is optional and, when supplied, must be an international E.164 number or a 10-digit Tanzanian local number.

```json
{
  "registrationNumber": "UDSM/2026/0142",
  "fullName": "Neema Mushi",
  "email": "neema.mushi@gmail.com",
  "phone": "+255712345678",
  "gender": "FEMALE",
  "dateOfBirth": "2003-05-14"
}
```

Errors use the shared API error format: `400 Bad Request` for invalid request data (including `fieldErrors`), `401 Unauthorized` for a missing or invalid token, `403 Forbidden` for an authenticated user without the required role, `404 Not Found` for an unknown student id, and `409 Conflict` for a duplicate registration number or email.

## Token Strategy

The access token is a signed HS256 JWT with the username as its subject, role claims, `type=access`, issue time, and a 15-minute expiration. Send it only as `Authorization: Bearer <accessToken>`.

The refresh token is a cryptographically random opaque value, not a JWT. The database stores only its SHA-256 hash. Refresh checks existence, revocation, and expiry under a row lock, revokes the old token, then issues a new refresh token and access token. A token can be used once; replaying a rotated or revoked token returns 401. A refresh token presented as a Bearer credential is not accepted.

Logout accepts `{ "refreshToken": "..." }` and revokes the token if it exists. Repeated logout or logout of an unknown token succeeds; a later refresh using that token returns 401. Passwords are stored only as BCrypt hashes and never returned by API responses.

## CORS, CSRF, and Request Flow

CORS allows only `app.cors.allowed-origin` (`http://localhost:3000`), methods GET/POST/PUT/DELETE/OPTIONS, and headers `Authorization` and `Content-Type`. Credentials are disabled; there are no wildcard origins.

CSRF is intentionally disabled because this is a stateless API that authenticates with Bearer tokens in an explicitly supplied header and sets no cookies. Browsers do not automatically attach that header cross-site, unlike cookies. Do not move tokens into cookies without revisiting this decision.

Request processing is **CORS -> JwtAuthenticationFilter -> authorization -> controller**. CORS handles preflight before route authorization. The JWT filter validates the signature, expiration, and access-token type, then reloads the user and authorities. Missing or invalid credentials leave the security context empty.

A **401 Unauthorized** means authentication is missing or invalid (including bad credentials and invalid/expired/reused refresh tokens). A **403 Forbidden** means the caller is authenticated but lacks the required role. API errors use `{timestamp, status, error, message, path, fieldErrors}`; validation errors include field-specific messages and unexpected errors never expose stack traces.

## Postman

Import `postman/secure-student-management.postman_collection.json` and `postman/secure-student-management.postman_environment.json`, then select the environment. Run the collection in order: it logs in as TEACHER and ADMIN, creates a disposable STUDENT account for the STUDENT list check, and exercises student CRUD and authorization. Set `adminPassword` and `teacherPassword` to the corresponding seed values from `application.properties` before running. The student setup uses a generated username and the test-only `studentPassword`.

The Postman files were not present in this checkout, so these files provide the requested student CRUD flow as a new collection rather than merging with an unavailable prior collection. Import it as a new collection or merge the requests manually if you have an earlier copy.

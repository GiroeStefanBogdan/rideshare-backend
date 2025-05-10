# Spring Boot Authentication Demo

A Spring Boot 3.4.5 application demonstrating both traditional email/password authentication and social login with Google OAuth2.

## Features

- Traditional email/password authentication with registration
- Social login with Google OAuth2
- BCrypt password encoding for secure password storage
- H2 database for user data storage
- Automatic distinction between users who register manually and users who log in via Google
- Custom login page and dashboard after successful login

## Prerequisites

- Java 21
- Maven
- Google Developer Account (for OAuth2)

## Configuration

### Google OAuth2 Configuration

1. Go to the [Google Developer Console](https://console.developers.google.com/)
2. Create a new project
3. Enable the Google+ API
4. Create OAuth2 credentials (OAuth client ID)
   - Application type: Web application
   - Authorized redirect URIs: `http://localhost:8080/login/oauth2/code/google`
5. Copy the client ID and client secret
6. Update the `application.properties` file with your Google client ID and client secret:

```properties
spring.security.oauth2.client.registration.google.client-id=your-google-client-id
spring.security.oauth2.client.registration.google.client-secret=your-google-client-secret
```

## Running the Application

1. Clone the repository
2. Configure Google OAuth2 as described above
3. Build the application:
   ```
   mvn clean package
   ```
4. Run the application:
   ```
   java -jar target/AuthenticationSpringBoot-0.0.1-SNAPSHOT.jar
   ```
5. Access the application at `http://localhost:8080`

## Usage

### Registration

1. Navigate to `http://localhost:8080/register`
2. Fill in the registration form with your name, email, and password
3. Click "Register"

### Login

1. Navigate to `http://localhost:8080/login`
2. Option 1: Enter your email and password, then click "Login"
3. Option 2: Click "Login with Google" and follow the Google authentication process

### Dashboard

After successful login, you will be redirected to the dashboard at `http://localhost:8080/dashboard`. The dashboard displays:

- Your profile information (name and email)
- The authentication method used (Email/Password or Google)

## H2 Database Console

The H2 database console is available at `http://localhost:8080/h2-console` with the following settings:

- JDBC URL: `jdbc:h2:mem:authdb`
- Username: `sa`
- Password: `password`

## API Endpoints

- `POST /register` - User registration
- `POST /login` - Traditional login
- `GET /oauth2/authorization/google` - Google OAuth2 login
- `GET /dashboard` - Dashboard page (authenticated users only)
- `GET /` - Home page
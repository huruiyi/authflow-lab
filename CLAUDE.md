# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a comprehensive OAuth2/OIDC demonstration project using Spring Authorization Server and Vue 3. The project implements and demonstrates various OAuth2 flows, security patterns, and authorization mechanisms for educational purposes.

### Architecture

**Backend**: Spring Boot 3.5.13 with Spring Authorization Server
- OAuth2/OIDC authorization server with MySQL persistence
- Multiple OAuth2 grant types: Authorization Code (+PKCE), Client Credentials, Device Code, Refresh Token
- OIDC features: UserInfo endpoint, RP-Initiated Logout, JWT claims customization
- Resource server endpoints protected by scopes
- Token lifecycle management: Introspection, Revocation
- Pushed Authorization Request (PAR) support
- Dynamic client registration and management

**Frontend**: Vue 3 + Vite + Element Plus
- Single-page application for OAuth2 flow demonstrations
- Interactive client management interface
- PKCE implementation with crypto.subtle and fallback
- Device code verification interface
- Custom consent and login pages

## Development Commands

### Backend (Java)

```bash
# Start the Spring Boot application
mvn -f backend/pom.xml spring-boot:run

# Run specific test
mvn -f backend/pom.xml test -Dtest=ClientServiceTest
```

### Frontend (Vue)

```bash
# Install dependencies (from project root)
npm install

# Start development server (local development)
npm --prefix frontend run dev

# Start with custom backend origin (for LAN testing)
VITE_BACKEND_ORIGIN=http://authlab.test:30000 npm --prefix frontend run dev

# Build for production
npm --prefix frontend run build
```

### Database Setup

The project uses MySQL. Configure connection in `backend/src/main/resources/application.yml`:

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/ssdemo?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
    username: root
    password: your_password
```

Schema initialization is handled automatically via `schema.sql` and `data.sql` on startup.

### Network Configuration

For LAN/mobile testing, update these values in `application.yml`:

```yaml
app:
  base-url: http://YOUR_IP:30000
  allowed-origins: http://localhost:5173,http://YOUR_IP:5173
  frontend-base-url: http://YOUR_IP:5173
```

Also update corresponding IP addresses in `backend/src/main/java/com/demo/authserver/config/DataInitializer.java`.

## Key Code Structure

### Backend Configuration

- **AuthorizationServerConfig**: OAuth2 server setup with JWT signing, JWK source, and authorization server settings
- **SecurityConfig**: Multi-filter-chain security configuration with CORS, custom device verification pages, and OIDC UserInfo mapping
- **DataInitializer**: Pre-populates demo OAuth2 clients and users on startup
- **ResourceServerConfig**: Resource server endpoint protection configuration

### Security Filter Chains

Two distinct security chains are configured:

1. **Order 1**: OAuth2 authorization server endpoints (`/oauth2/**`, `/.well-known/**`)
   - Handles authorization, token, device authorization, PAR, introspection, revocation
   - Custom consent page at `/oauth2/consent`
   - Custom device verification at `/activate`

2. **Order 3**: Default application security
   - API endpoints (`/api/**`) require authentication
   - Custom login page at `/login`
   - CSRF disabled for API endpoints

### OAuth2 Clients (Pre-configured)

- `spa-public-client`: SPA with PKCE, no client secret (Authorization Code + Refresh)
- `webapp-client`: Traditional web app with client secret (Authorization Code + Refresh)
- `m2m-service-client`: Machine-to-machine service (Client Credentials)
- `device-flow-client`: IoT device flow (Device Code + Refresh)
- `spa-rotation-client`: SPA with short-lived tokens and refresh rotation
- `all-in-one-client`: Multi-grant client for testing various flows
- `spa-par-client`: Client configured for PAR demonstration

### PKCE Implementation

Frontend implements PKCE in `frontend/src/composables/pkce.js`:
- Uses native `crypto.subtle` for SHA-256 when available
- Falls back to pure JavaScript SHA-256 implementation
- Generates secure random code_verifier (96 characters)
- Creates code_challenge via base64url-encoded SHA-256 hash

### Frontend API Layer

`frontend/src/api/oauth2.js` provides centralized OAuth2 API methods:
- Discovery, JWKS, token exchange
- Device authorization and polling
- PAR requests (relayed through backend)
- Token introspection/revocation
- UserInfo and resource server calls

### Router & Authentication Flow

Vue Router in `frontend/src/router/index.js`:
- Authentication guard for protected routes (`/flows`, `/clients`)
- Auto-redirect to login with returnTo query parameter
- Routes for all OAuth2 callbacks and custom pages

## Important Implementation Details

### Client Registration Pattern

The project uses JDBC-based client persistence with a workaround pattern:
- Updates require deleting and re-inserting the record (same ID)
- This is because Spring's `RegisteredClientRepository` doesn't support partial updates
- See `ClientService.update()` method for implementation

### Token Configuration

Different clients demonstrate different token strategies:
- Standard: 5min access token, 1h refresh token, reuse refresh tokens
- Short-lived: 30s access token, 10min refresh token, refresh rotation
- Long-lived: 1h access token, 7d refresh token, no reuse

### Device Code Flow

Custom device verification UI at `/activate`:
- Shows device code input and countdown timer
- Auto-redirects via `verification_uri_complete` when available
- Maps user_code to client_id and scope for consent

### Pushed Authorization Request (PAR)

PAR is implemented as a backend relay:
- Frontend calls `/api/auth/par` (no client_secret exposed)
- Backend forwards to `/oauth2/par` with proper client authentication
- Returns `request_uri` for frontend to use in authorization request
- Client: `spa-par-client` configured in `application.yml`

### CORS Configuration

CORS is configured dynamically from `app.allowed-origins`:
- Accepts comma-separated list of origins
- Supports both local and LAN origins
- Allows credentials and all standard HTTP methods

### Testing Scripts

- `backend/oauth2-demo-curl.sh`: Comprehensive curl examples for all OAuth2 flows
- `backend/verify-par-flow.ps1`: PowerShell script for PAR flow verification
- `backend/postman_collection.json`: Postman collection for API testing

## Common Work Patterns

### Adding a New OAuth2 Flow

1. Add client configuration in `DataInitializer.java`
2. Update `data.sql` if needed
3. Add UI controls in `frontend/src/views/FlowsDemo.vue`
4. Implement flow logic using existing API methods in `oauth2.js`

### Modifying Scopes or Claims

1. Update client scope lists in `DataInitializer.java`
2. Modify OIDC UserInfo mapping in `SecurityConfig.securityFilterChain()`
3. Update resource endpoint scope requirements in `ResourceController`

### Debugging OAuth2 Flows

- Enable DEBUG logging in `application.yml` for Spring Security
- Check `oauth2_authorization` table for flow state
- Use `/resource/token-info` endpoint to inspect JWT claims
- Monitor browser console for PKCE flow issues

### Custom Consent Page

- Located at `frontend/src/views/ConsentView.vue`
- Endpoint configured in `SecurityConfig` via `.consentPage("/oauth2/consent")`
- Must handle POST to consent approval URL

## Security Considerations

- Demo passwords are stored with `{noop}` encoding (use `{bcrypt}` in production)
- Client secrets in `data.sql` are plain text (encode in production)
- Short token lifetimes are for demonstration only
- PKCE is enforced for all public clients
- CORS is properly configured but test origins should be restricted in production
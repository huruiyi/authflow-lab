### Discovery
curl -i http://localhost:30000/.well-known/openid-configuration

### JWK Set
curl -i http://localhost:30000/oauth2/jwks

### Public resource
curl -i http://localhost:30000/resource/public

### Client Credentials -> access token
curl -i -X POST http://localhost:30000/oauth2/token \
  -u m2m-service-client:m2m-secret \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=client_credentials&scope=read write"

### Read protected resource with M2M token
export M2M_ACCESS_TOKEN="replace-with-access-token"
curl -i http://localhost:30000/resource/read \
  -H "Authorization: Bearer $M2M_ACCESS_TOKEN"

### Write protected resource with M2M token
curl -i -X POST http://localhost:30000/resource/write \
  -H "Authorization: Bearer $M2M_ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"batchNo":"SYNC-001","operation":"inventory_update","source":"curl-script"}'

### Token claims
curl -i http://localhost:30000/resource/token-info \
  -H "Authorization: Bearer $M2M_ACCESS_TOKEN"

### Device Authorization -> device_code
curl -i -X POST http://localhost:30000/oauth2/device_authorization \
  -u device-flow-client:device-secret \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "client_id=device-flow-client&scope=openid profile read"

### Device token polling
export DEVICE_CODE="replace-with-device-code"
curl -i -X POST http://localhost:30000/oauth2/token \
  -u device-flow-client:device-secret \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=urn:ietf:params:oauth:grant-type:device_code&device_code=$DEVICE_CODE&client_id=device-flow-client"

### Refresh token
export REFRESH_TOKEN="replace-with-refresh-token"
curl -i -X POST http://localhost:30000/oauth2/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=refresh_token&client_id=spa-public-client&refresh_token=$REFRESH_TOKEN"

### UserInfo
export USER_ACCESS_TOKEN="replace-with-user-access-token"
curl -i http://localhost:30000/userinfo \
  -H "Authorization: Bearer $USER_ACCESS_TOKEN"

### Profile resource
curl -i http://localhost:30000/resource/profile \
  -H "Authorization: Bearer $USER_ACCESS_TOKEN"

### Read resource with user token
curl -i http://localhost:30000/resource/read \
  -H "Authorization: Bearer $USER_ACCESS_TOKEN"

### Write resource with user token
curl -i -X POST http://localhost:30000/resource/write \
  -H "Authorization: Bearer $USER_ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"orderNo":"ORD-1001","amount":299,"status":"PAID"}'

### PAR end-to-end (Pushed Authorization Request + PKCE)
export AS_BASE_URL="http://localhost:30000"
export PAR_CLIENT_ID="spa-par-client"
export PAR_REDIRECT_URI="http://localhost:5173/callback"
export PAR_SCOPE="openid profile email read write"
export PAR_STATE="par-demo-001"

### 1) Generate PKCE code_verifier / code_challenge
export PAR_CODE_VERIFIER="$(openssl rand -base64 96 | tr -d '=+/' | cut -c1-64)"
export PAR_CODE_CHALLENGE="$(printf '%s' "$PAR_CODE_VERIFIER" | openssl dgst -binary -sha256 | openssl base64 | tr '+/' '-_' | tr -d '=')"

### 2) Push authorization request and obtain request_uri
export PAR_RESPONSE_JSON="$(curl -s -X POST "$AS_BASE_URL/oauth2/par" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "response_type=code" \
  -d "client_id=$PAR_CLIENT_ID" \
  -d "redirect_uri=$PAR_REDIRECT_URI" \
  -d "scope=$PAR_SCOPE" \
  -d "state=$PAR_STATE" \
  -d "code_challenge=$PAR_CODE_CHALLENGE" \
  -d "code_challenge_method=S256")"

echo "$PAR_RESPONSE_JSON"
export PAR_REQUEST_URI="$(echo "$PAR_RESPONSE_JSON" | sed -n 's/.*"request_uri"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p')"

### 3) Build authorization URL with request_uri (open in browser)
echo "Open this URL in browser to login/consent:"
echo "$AS_BASE_URL/oauth2/authorize?client_id=$PAR_CLIENT_ID&request_uri=$PAR_REQUEST_URI"

### 4) After callback, copy code from redirect_uri and exchange token
export PAR_AUTH_CODE="replace-with-authorization-code"
curl -i -X POST "$AS_BASE_URL/oauth2/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=authorization_code" \
  -d "client_id=$PAR_CLIENT_ID" \
  -d "code=$PAR_AUTH_CODE" \
  -d "redirect_uri=$PAR_REDIRECT_URI" \
  -d "code_verifier=$PAR_CODE_VERIFIER"

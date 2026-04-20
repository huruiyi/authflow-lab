### Discovery
curl -i http://localhost:9000/.well-known/openid-configuration

### JWK Set
curl -i http://localhost:9000/oauth2/jwks

### Public resource
curl -i http://localhost:9000/resource/public

### Client Credentials -> access token
curl -i -X POST http://localhost:9000/oauth2/token \
  -u m2m-service-client:m2m-secret \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=client_credentials&scope=read write"

### Read protected resource with M2M token
export M2M_ACCESS_TOKEN="replace-with-access-token"
curl -i http://localhost:9000/resource/read \
  -H "Authorization: Bearer $M2M_ACCESS_TOKEN"

### Write protected resource with M2M token
curl -i -X POST http://localhost:9000/resource/write \
  -H "Authorization: Bearer $M2M_ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"batchNo":"SYNC-001","operation":"inventory_update","source":"curl-script"}'

### Token claims
curl -i http://localhost:9000/resource/token-info \
  -H "Authorization: Bearer $M2M_ACCESS_TOKEN"

### Device Authorization -> device_code
curl -i -X POST http://localhost:9000/oauth2/device_authorization \
  -u device-flow-client:device-secret \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "client_id=device-flow-client&scope=openid profile read"

### Device token polling
export DEVICE_CODE="replace-with-device-code"
curl -i -X POST http://localhost:9000/oauth2/token \
  -u device-flow-client:device-secret \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=urn:ietf:params:oauth:grant-type:device_code&device_code=$DEVICE_CODE&client_id=device-flow-client"

### Refresh token
export REFRESH_TOKEN="replace-with-refresh-token"
curl -i -X POST http://localhost:9000/oauth2/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=refresh_token&client_id=spa-public-client&refresh_token=$REFRESH_TOKEN"

### UserInfo
export USER_ACCESS_TOKEN="replace-with-user-access-token"
curl -i http://localhost:9000/userinfo \
  -H "Authorization: Bearer $USER_ACCESS_TOKEN"

### Profile resource
curl -i http://localhost:9000/resource/profile \
  -H "Authorization: Bearer $USER_ACCESS_TOKEN"

### Read resource with user token
curl -i http://localhost:9000/resource/read \
  -H "Authorization: Bearer $USER_ACCESS_TOKEN"

### Write resource with user token
curl -i -X POST http://localhost:9000/resource/write \
  -H "Authorization: Bearer $USER_ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"orderNo":"ORD-1001","amount":299,"status":"PAID"}'

import axios from 'axios'

const jsonHttp = axios.create({
  withCredentials: true,
  headers: { 'Content-Type': 'application/json' }
})

const formHttp = axios.create({
  headers: { 'Content-Type': 'application/x-www-form-urlencoded' }
})

export const oauth2Api = {
  getDiscovery: () => jsonHttp.get('/.well-known/openid-configuration'),
  getJwks: () => jsonHttp.get('/oauth2/jwks'),
  exchangeCode: body => formHttp.post('/oauth2/token', new URLSearchParams(body).toString()),
  refreshToken: body => formHttp.post('/oauth2/token', new URLSearchParams(body).toString()),
  clientCredentials: (body, headers = {}) => formHttp.post(
    '/oauth2/token',
    new URLSearchParams(body).toString(),
    { headers }
  ),
  deviceAuthorize: (body, headers = {}) => formHttp.post(
    '/oauth2/device_authorization',
    new URLSearchParams(body).toString(),
    { headers }
  ),
  pollDeviceToken: (body, headers = {}) => formHttp.post(
    '/oauth2/token',
    new URLSearchParams(body).toString(),
    { headers }
  ),
  introspectToken: (body, headers = {}) => formHttp.post(
    '/oauth2/introspect',
    new URLSearchParams(body).toString(),
    { headers }
  ),
  revokeToken: (body, headers = {}) => formHttp.post(
    '/oauth2/revoke',
    new URLSearchParams(body).toString(),
    { headers }
  ),
  getUserInfo: token => jsonHttp.get('/userinfo', {
    headers: { Authorization: `Bearer ${token}` }
  }),
  callPublicResource: () => jsonHttp.get('/resource/public'),
  callProfileResource: token => jsonHttp.get('/resource/profile', {
    headers: { Authorization: `Bearer ${token}` }
  }),
  callReadResource: token => jsonHttp.get('/resource/read', {
    headers: { Authorization: `Bearer ${token}` }
  }),
  callWriteResource: (token, body) => jsonHttp.post('/resource/write', body, {
    headers: { Authorization: `Bearer ${token}` }
  }),
  callTokenInfo: token => jsonHttp.get('/resource/token-info', {
    headers: { Authorization: `Bearer ${token}` }
  })
}

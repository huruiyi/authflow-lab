export function generateRandomString(length = 64) {
  const chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-._~'
  const array = new Uint8Array(length)
  crypto.getRandomValues(array)
  return Array.from(array, x => chars[x % chars.length]).join('')
}

async function sha256(plain) {
  const encoder = new TextEncoder()
  const data = encoder.encode(plain)
  return await crypto.subtle.digest('SHA-256', data)
}

function base64UrlEncode(buffer) {
  return btoa(String.fromCharCode(...new Uint8Array(buffer)))
    .replace(/\+/g, '-')
    .replace(/\//g, '_')
    .replace(/=+$/g, '')
}

export async function generatePkcePair() {
  const codeVerifier = generateRandomString(96)
  const hashed = await sha256(codeVerifier)
  const codeChallenge = base64UrlEncode(hashed)
  return { codeVerifier, codeChallenge }
}

import axios from 'axios'

function redirectToLogin() {
  const returnTo = `${window.location.pathname}${window.location.search}${window.location.hash}`
  const params = new URLSearchParams({ returnTo })
  window.location.href = `/login?${params.toString()}`
}

const http = axios.create({
  withCredentials: true,
  headers: { 'Content-Type': 'application/json' }
})

http.interceptors.response.use(
  res => res,
  err => {
    if (err.response?.status === 401) {
      redirectToLogin()
    }
    return Promise.reject(err)
  }
)

export const authApi = {
  login: (username, password) =>
    http.post('/api/auth/login', { username, password }),
  logout: () => http.post('/api/auth/logout'),
  status: () => http.get('/api/auth/status')
}

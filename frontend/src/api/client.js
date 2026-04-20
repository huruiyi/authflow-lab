import axios from 'axios'

const http = axios.create({
  withCredentials: true,
  headers: { 'Content-Type': 'application/json' }
})

http.interceptors.response.use(
  res => res,
  err => {
    if (err.response?.status === 401) {
      window.location.href = '/login'
    }
    return Promise.reject(err)
  }
)

export const clientApi = {
  findAll: () => http.get('/api/clients'),
  findById: id => http.get(`/api/clients/${id}`),
  create: data => http.post('/api/clients', data),
  update: (id, data) => http.put(`/api/clients/${id}`, data),
  remove: id => http.delete(`/api/clients/${id}`)
}

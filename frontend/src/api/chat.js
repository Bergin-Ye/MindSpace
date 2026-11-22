import http from './http'

export const askQuestion = (payload) =>
  http.post('/chat/ask', payload, { timeout: 180000 })

export const listSessions = (params) => http.get('/chat/sessions', { params })

export const listMessages = (sessionId, params) =>
  http.get(`/chat/sessions/${sessionId}/messages`, { params })

export const renameSession = (id, title) => http.put(`/chat/sessions/${id}`, { title })

export const deleteSession = (id) => http.delete(`/chat/sessions/${id}`)

import http from './http'

export const listKnowledgeBases = () => http.get('/knowledge-bases')

export const getKnowledgeBase = (id) => http.get(`/knowledge-bases/${id}`)

export const createKnowledgeBase = (payload) => http.post('/knowledge-bases', payload)

export const updateKnowledgeBase = (id, payload) =>
  http.put(`/knowledge-bases/${id}`, payload)

export const deleteKnowledgeBase = (id) => http.delete(`/knowledge-bases/${id}`)

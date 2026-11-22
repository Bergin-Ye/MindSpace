import http from './http'

export const listDocuments = (params) => http.get('/documents', { params })

export const getDocument = (id) => http.get(`/documents/${id}`)

export const uploadDocument = (formData, onProgress) =>
  http.post('/documents/upload', formData, {
    timeout: 120000,
    onUploadProgress: (event) => {
      if (!onProgress) return
      const total = event.total || 0
      onProgress(total ? Math.round((event.loaded * 100) / total) : 0)
    },
  })

export const renameDocument = (id, originalName) =>
  http.put(`/documents/${id}`, { originalName })

export const moveDocument = (id, folderId) =>
  http.put(`/documents/${id}/move`, { folderId })

export const deleteDocument = (id) => http.delete(`/documents/${id}`)

export const downloadDocument = (id) =>
  http.get(`/documents/${id}/download`, {
    responseType: 'blob',
    timeout: 120000,
  })

export const parseDocument = (id) =>
  http.post(`/documents/${id}/parse`, null, { timeout: 120000 })

export const parseDocumentAsync = (id) =>
  http.post(`/documents/${id}/parse-async`, null, { timeout: 30000 })

export const getParseStatus = (id) => http.get(`/documents/${id}/parse-status`)

export const searchDocuments = (params) => http.get('/documents/search', { params })

export const embedDocument = (id) =>
  http.post(`/documents/${id}/embed`, null, { timeout: 180000 })

export const vectorSearch = (params) =>
  http.get('/documents/vector-search', { params, timeout: 90000 })

export const analyzeDocument = (id) =>
  http.post(`/documents/${id}/analyze`, null, { timeout: 180000 })

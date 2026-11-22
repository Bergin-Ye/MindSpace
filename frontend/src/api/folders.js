import http from './http'

export const listFolders = (knowledgeBaseId) =>
  http.get('/folders', { params: { knowledgeBaseId } })

export const folderTree = (knowledgeBaseId) =>
  http.get('/folders/tree', { params: { knowledgeBaseId } })

export const createFolder = (payload) => http.post('/folders', payload)

export const renameFolder = (id, name) => http.put(`/folders/${id}`, { name })

export const moveFolder = (id, parentId) => http.put(`/folders/${id}/move`, { parentId })

export const deleteFolder = (id) => http.delete(`/folders/${id}`)

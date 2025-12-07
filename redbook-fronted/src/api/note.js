import request from './request'

/**
 * 获取笔记列表（默认推荐）
 * @param {number} page - 页码
 * @param {number} pageSize - 每页数量
 * @param {string} type - 笔记类型（可选）
 * @returns {Promise}
 */
export const getNoteList = (page, pageSize, type) => {
  return request.get('/note/getNoteList', {
    params: {
      page,
      pageSize,
      type
    }
  })
}

/**
 * 根据位置获取笔记列表
 * @param {string} longitude - 经度
 * @param {string} latitude - 纬度
 * @returns {Promise}
 */
export const getNoteListByLocation = (longitude, latitude) => {
  return request.get('/note/getNoteListByLocation', {
    params: {
      longitude,
      latitude
    }
  })
}

/**
 * 获取指定用户发布的笔记
 * @param {number} userId - 用户ID
 * @param {number} page - 页码
 * @param {number} pageSize - 每页数量
 * @returns {Promise}
 */
export const getNoteListByUserId = (userId, page, pageSize) => {
  return request.get('/note/getNoteListByUserId', {
    params: {
      userId,
      page,
      pageSize
    }
  })
}

/**
 * 获取指定用户收藏的笔记
 * @param {number} userId - 用户ID
 * @param {number} page - 页码
 * @param {number} pageSize - 每页数量
 * @returns {Promise}
 */
export const getNoteListByCollectionUserId = (userId, page, pageSize) => {
  return request.get('/note/getNoteListByCollectionUserId', {
    params: {
      userId,
      page,
      pageSize
    }
  })
}

/**
 * 获取自己发布的笔记列表
 * @returns {Promise}
 */
export const getNoteListByOwn = () => {
  return request.get('/note/getNoteListByOwn')
}

/**
 * 获取浏览过的笔记列表
 * @returns {Promise}
 */
export const getNoteListByScan = () => {
  return request.get('/note/getNoteListByScan')
}

/**
 * 获取关注用户的笔记列表
 * @returns {Promise}
 */
export const getNoteListByAttention = () => {
  return request.get('/note/getNoteListByAttention')
}

/**
 * 获取收藏的笔记列表
 * @returns {Promise}
 */
export const getNoteByCollection = () => {
  return request.get('/note/getNoteByCollection')
}

/**
 * 获取点赞的笔记列表
 * @returns {Promise}
 */
export const getNoteByLike = () => {
  return request.get('/note/getNoteByLike')
}

/**
 * 点赞/取消点赞笔记
 * @param {number} noteId - 笔记ID
 * @returns {Promise}
 */
export const likeNote = (noteId) => {
  return request.put(`/note/like/${noteId}`)
}

/**
 * 检查是否已点赞
 * @param {number} noteId - 笔记ID
 * @returns {Promise}
 */
export const isLike = (noteId) => {
  return request.get(`/note/isLike/${noteId}`)
}

/**
 * 收藏/取消收藏笔记
 * @param {number} noteId - 笔记ID
 * @returns {Promise}
 */
export const collectNote = (noteId) => {
  return request.put(`/note/collection/${noteId}`)
}

/**
 * 检查是否已收藏
 * @param {number} noteId - 笔记ID
 * @returns {Promise}
 */
export const isCollection = (noteId) => {
  return request.get(`/note/isCollection/${noteId}`)
}
/**
 * 获取笔记详情
 * @param {number} noteId - 笔记ID
 * @returns {Promise}
 */
export const getNote = (noteId) => {
  return request.get(`/note/getNote/${noteId}`)
}

/**
 * 发布笔记
 * @param {FormData} formData - 包含图片、标题、内容等的表单数据
 * @returns {Promise}
 */
export const postNote = (formData) => {
  return request.post('/note/postNote', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

/**
 * 更新笔记
 * @param {FormData} formData - 包含图片、标题、内容、ID等的表单数据
 * @returns {Promise}
 */
export const updateNote = (formData) => {
  return request.put('/note/updateNote', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

/**
 * 删除笔记
 * @param {number} noteId - 笔记ID
 * @returns {Promise}
 */
export const deleteNote = (noteId) => {
  return request.delete(`/note/deleteNote/${noteId}`)
}

/**
 * 批量删除笔记
 * @param {Array<number>} noteIds - 笔记ID列表
 * @returns {Promise}
 */
export const batchDeleteNotes = (noteIds) => {
  return request.delete('/note/batchDeleteNotes', {
    data: noteIds
  })
}

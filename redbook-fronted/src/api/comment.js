import request from './request'

/**
 * 获取评论列表
 * @param {number} noteId - 笔记ID
 * @returns {Promise}
 */
export const getCommentList = (noteId) => {
  return request.get(`/comment/getCommentList/${noteId}`)
}

/**
 * 发布评论
 * @param {Object} data - 评论数据 { noteId, content, parentId }
 * @returns {Promise}
 */
export const postComment = (data) => {
  return request.post('/comment/postComment', data)
}

/**
 * 点赞评论
 * @param {number} commentId - 评论ID
 * @returns {Promise}
 */
export const likeComment = (commentId) => {
  return request.post(`/comment/like/${commentId}`)
}

/**
 * 取消点赞评论
 * @param {number} commentId - 评论ID
 * @returns {Promise}
 */
export const unlikeComment = (commentId) => {
  return request.post(`/comment/unlike/${commentId}`)
}

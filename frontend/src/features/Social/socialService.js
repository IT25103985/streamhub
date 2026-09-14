import axiosInstance from '../../api/axiosInstance';

export //api client for comments and like reaction requests
const socialService = {
  //user comment and like api calls
  addComment: async (videoId, data) => {
    const response = await axiosInstance.post(`/videos/${videoId}/comments`, data);
    return response.data;
  },
  getCommentsByVideo: async (videoId) => {
    const response = await axiosInstance.get(`/videos/${videoId}/comments`);
    return response.data;
  },
  deleteComment: async (commentId) => {
    const response = await axiosInstance.delete(`/comments/${commentId}`);
    return response.data;
  },
  updateComment: async (commentId, data) => {
    const response = await axiosInstance.patch(`/comments/${commentId}`, data);
    return response.data;
  },
  toggleLike: async (videoId) => {
    const response = await axiosInstance.post(`/videos/${videoId}/like`);
    return response.data;
  },
  getLikeStatus: async (videoId) => {
    const response = await axiosInstance.get(`/videos/${videoId}/like-status`);
    return response.data;
  },

  //admin comment moderation api calls
  getAllCommentsAdmin: async () => {
    const response = await axiosInstance.get('/admin/comments');
    return response.data;
  },
  deleteCommentAdmin: async (commentId) => {
    const response = await axiosInstance.delete(`/admin/comments/${commentId}`);
    return response.data;
  }
};

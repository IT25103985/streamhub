import axiosInstance from './axiosInstance';

//api service for help center topic endpoints
export const helpApi = {
  getAllTopics: async () => {
    const response = await axiosInstance.get('/help/topics');
    return response.data;
  },

  createTopic: async (topicData) => {
    const response = await axiosInstance.post('/help/topics', topicData);
    return response.data;
  },

  updateTopic: async (id, topicData) => {
    const response = await axiosInstance.put(`/help/topics/${id}`, topicData);
    return response.data;
  },

  deleteTopic: async (id) => {
    const response = await axiosInstance.delete(`/help/topics/${id}`);
    return response.data;
  }
};

import axiosInstance from './axiosInstance';

//api methods for user follow relationships and counts
export const followUser = async (userId) => {
  const response = await axiosInstance.post(`/follows/${userId}`);
  return response.data;
};

export const unfollowUser = async (userId) => {
  const response = await axiosInstance.delete(`/follows/${userId}`);
  return response.data;
};

export const getFollowCounts = async (userId) => {
  const response = await axiosInstance.get(`/follows/${userId}/counts`);
  return response.data;
};

export const getFollowers = async (userId, page = 0, size = 10) => {
  const response = await axiosInstance.get(`/follows/${userId}/followers`, {
    params: { page, size }
  });
  return response.data;
};

export const getFollowing = async (userId, page = 0, size = 10) => {
  const response = await axiosInstance.get(`/follows/${userId}/following`, {
    params: { page, size }
  });
  return response.data;
};
